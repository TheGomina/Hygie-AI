"""Local LLM orchestration : chargement des modèles pré-téléchargés.

Si le modèle local n’est pas trouvé, bascule sur l’API Hugging Face comme
secours. Trois modèles disponibles : BioMistral, HippoMistral, MedFound-LLaMA3.
"""

from __future__ import annotations

import json
import os
from functools import lru_cache
import functools
from prometheus_client import Histogram
from pathlib import Path
from typing import Dict, List, Tuple
from collections import Counter

# === Chemins locaux vers les poids LLM =======================================
# Mise à jour après téléchargement dans C:\Users\ssebb\LLM
# Ajustez au besoin si vous changez d’emplacement.
from .config import settings

_BASE_DIR = Path(settings.llm_base_dir)


# Découverte automatique des modèles présents dans le dossier monté.
# Un modèle est comptabilisé si le répertoire contient un fichier poids
# typique (.bin ou .safetensors).
def _discover_models() -> Dict[str, Path]:  # ≤15 lignes pour respecter règle 4
    mapping: Dict[str, Path] = {}
    if not _BASE_DIR.exists():
        return mapping

    for d in _BASE_DIR.iterdir():
        if not d.is_dir():
            continue
        # heuristique simple : présence d'un fichier poids ⇒ modèle valide
        has_weights = any(
            (d / fn).is_file()
            for fn in [
                "pytorch_model.bin",
                "model.safetensors",
                "model.safetensors.index.json",
            ]
        )
        if has_weights:
            key = d.name.lower().replace("-", "").replace("_", "")
            mapping[key] = d
    return mapping


# Modèles découverts + alias fixes pour compat ascendante
MODEL_PATHS: Dict[str, Path] = _discover_models() or {
    "biomistral": _BASE_DIR / "biomistral-7b",
    "hippomistral": _BASE_DIR / "hippomistral",
    "medfound": _BASE_DIR / "medfound-llama3-8b",
}


# Normalise les clés pour accepter par ex. "biomistral7b" ou "biomistral-7b".
def _resolve_name(name: str) -> str:
    # Normalize input and available model keys for matching
    slug = name.lower().replace("-", "").replace("_", "")
    for orig_key in MODEL_PATHS:
        normalized_key = orig_key.lower().replace("-", "").replace("_", "")
        if normalized_key == slug or normalized_key.startswith(slug):
            return orig_key
    # défaut : premier modèle disponible
    if MODEL_PATHS:
        return next(iter(MODEL_PATHS))
    raise ValueError("Aucun modèle local disponible")


# Fallback (rare) : modèle HF public
HF_API_MODEL = "mistralai/BioMistral-7B-v0.1"
HF_API_KEY = settings.hf_api_key

# ----------------------------------------------------------------------------

# Histogram for orchestration latency by stage
LLM_ORCH_HISTOGRAM = Histogram(
    'hygie_llm_orchestration_seconds',
    'Latency of LLM orchestration stages',
    ['stage'],
)

def track(stage: str):
    """Decorator to track function latency with Prometheus histogram."""
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            with LLM_ORCH_HISTOGRAM.labels(stage=stage).time():
                return func(*args, **kwargs)
        return wrapper
    return decorator

@lru_cache(maxsize=3)
def _load_model(name: str) -> Tuple[AutoTokenizer, AutoModelForCausalLM]:
    """Charge *lazy* le tokenizer et le modèle."""
    import torch
    from transformers import AutoModelForCausalLM, AutoTokenizer  # type: ignore

    path = MODEL_PATHS.get(name)
    if path is None or not path.exists():
        raise FileNotFoundError(f"Chemin modèle introuvable : {path}")

    try:
        tokenizer = AutoTokenizer.from_pretrained(path, use_fast=True)
        try:
            model = AutoModelForCausalLM.from_pretrained(
                path,
                torch_dtype=torch.float16,
                device_map="auto",
            )
        except RuntimeError as e:
            msg = str(e)
            # Legacy single-file checkpoints → retry with weights_only=False
            if "weights_only" in msg or "Weights only load failed" in msg:
                model = AutoModelForCausalLM.from_pretrained(
                    path,
                    torch_dtype=torch.float16,
                    device_map="auto",
                    weights_only=False,
                )
            # TensorFlow checkpoints (.ckpt) → retry with from_tf=True
            elif "from_tf" in msg or "TF 2.0" in msg:
                model = AutoModelForCausalLM.from_pretrained(
                    path,
                    torch_dtype=torch.float16,
                    device_map="auto",
                    from_tf=True,
                )
            else:
                raise
    except Exception as exc:  # pragma: no cover
        raise RuntimeError(f"Erreur chargement modèle local : {exc}") from exc

    return tokenizer, model


@lru_cache(maxsize=128)
def _cached_local_generate(
    name: str, prompt_hash: str, prompt: str
) -> str:  # noqa: D401
    """Génère en local et met en cache le résultat."""
    import torch
    from transformers import AutoModelForCausalLM, AutoTokenizer  # type: ignore

    tok, mdl = _load_model(name)
    inputs = tok(prompt, return_tensors="pt").to(mdl.device)
    with torch.no_grad():
        out = mdl.generate(
            **inputs,
            max_new_tokens=256,
            do_sample=False,  # déterministe
            temperature=0.0,
        )

    # ------------------------------------------------------------------
    # Décodage : on retire le prompt pour ne garder que la génération.
    # Hygie règle 4 (≤50 lignes) : gardons ce bloc court et lisible.
    # ------------------------------------------------------------------
    full = out[0]
    prompt_len = inputs["input_ids"].shape[-1]
    gen_tokens = full[prompt_len:]
    if gen_tokens.numel() == 0:  # rien généré → fallback texte complet
        gen_tokens = full

    text = tok.decode(gen_tokens, skip_special_tokens=True).strip()
    return text


def list_local_models() -> List[str]:  # noqa: D401
    """Retourne la liste des modèles locaux réellement présents."""
    return sorted([n for n, p in MODEL_PATHS.items() if p.exists()])


# ----------------------------------------------------------------------------


def _hf_generate(prompt: str) -> str:
    """Secours : appel API HF si modèle local indisponible."""
    if not HF_API_KEY:
        return "[LLM] Modèle local non trouvé et HF_API_KEY absent."

    import httpx  # lazy import

    url = f"https://api-inference.huggingface.co/models/{HF_API_MODEL}"
    headers = {"Authorization": f"Bearer {HF_API_KEY}"}
    payload = {
        "inputs": prompt,
        "parameters": {
            "max_new_tokens": 256,
            "temperature": 0.0,
            "return_full_text": False,
        },
    }
    try:
        data = httpx.post(url, json=payload, headers=headers, timeout=45).json()
    except Exception as exc:  # pragma: no cover
        return f"[HF erreur] {exc}"

    if isinstance(data, list) and data and "generated_text" in data[0]:
        return data[0]["generated_text"]
    return str(data)[:500]


@track('ensemble_generate')
def ensemble_generate(prompt: str) -> str:
    """Ensemble generation: calls each local LLM and selects consensus."""
    prompt_hash = str(hash(prompt))
    # Fallback if no local models
    if not MODEL_PATHS:
        return _hf_generate(prompt)
    responses: List[str] = []
    for name in MODEL_PATHS:
        try:
            resp = _cached_local_generate(name, prompt_hash, prompt)
        except Exception:
            resp = _hf_generate(prompt)
        responses.append(resp)
    counts = Counter(responses)
    # Return most common response
    return counts.most_common(1)[0][0]


@track('cascade_generate')
def cascade_generate(prompt: str) -> str:  # noqa: D401
    """Pipeline cascade : BioMistral → MedFound → Hippomistral."""
    # Stage 1: synthesis
    try:
        resolved = _resolve_name('biomistral')
        summary = _cached_local_generate(resolved, str(hash(prompt)), prompt)
    except Exception:
        summary = _hf_generate(prompt)

    # Stage 2: factual analysis
    analysis_prompt = (
        f"Analyse factuelle de la synthèse suivante et détecte interactions ou erreurs :\n{summary}"
    )
    try:
        resolved2 = _resolve_name('medfound')
        analysis = _cached_local_generate(resolved2, str(hash(analysis_prompt)), analysis_prompt)
    except Exception:
        analysis = _hf_generate(analysis_prompt)

    # Stage 3: reformulation finale
    reformulation_prompt = (
        f"Reformule de façon claire, structurée et exhaustive :\n{analysis}"
    )
    try:
        resolved3 = _resolve_name('hippomistral')
        return _cached_local_generate(resolved3, str(hash(reformulation_prompt)), reformulation_prompt)
    except Exception:
        return _hf_generate(reformulation_prompt)


@track('hybrid_generate_summary')
def hybrid_generate_summary(
    demographics: Dict[str, str | int],
    medications: List[str],
    problems: List[str]
) -> str:  # noqa: D401
    """Hybrid pipeline: cascade + ensemble across LLMs for BMP summary."""
    # build base prompt
    base_prompt = (
        "Vous êtes un pharmacien clinicien expert.\n"
        "Analysez la liste médicamenteuse et fournissez : \n"
        "1. Synthèse du traitement. \n"
        "2. Problèmes identifiés. \n"
        "3. Recommandations priorisées.\n\n"
        f"Données patient : {json.dumps(demographics, ensure_ascii=False)}\n"
        f"Médicaments : {', '.join(medications)}\n"
        f"Problèmes (règles fixes) : {', '.join(problems) if problems else 'Aucun'}\n"
        "Réponds en français, bullet points."
    )
    # Stage 1: synthesis
    summary = ensemble_generate(base_prompt)
    # Stage 2: factual analysis
    analysis_prompt = (
        f"Analyse factuelle de la synthèse suivante et détecte interactions ou erreurs :\n{summary}"
    )
    analysis = ensemble_generate(analysis_prompt)
    # Stage 3: reformulation finale
    reformulation_prompt = (
        f"Reformule de façon claire, structurée et exhaustive :\n{analysis}"
    )
    return ensemble_generate(reformulation_prompt)


def generate_summary(  # noqa: D401
    demographics: Dict[str, str | int],
    medications: List[str],
    problems: List[str],
    model_name: str = "biomistral",
) -> str:
    """Génère une synthèse BMP via une orchestration LLM (hybrid, ensemble, cascade)."""
    # Construire le prompt de base
    prompt = (
        "Vous êtes un pharmacien clinicien expert.\n"
        "Analysez la liste médicamenteuse et fournissez : \n"
        "1. Synthèse du traitement. \n"
        "2. Problèmes identifiés. \n"
        "3. Recommandations priorisées.\n\n"
        f"Données patient : {json.dumps(demographics, ensure_ascii=False)}\n"
        f"Médicaments : {', '.join(medications)}\n"
        f"Problèmes (règles fixes) : {', '.join(problems) if problems else 'Aucun'}\n"
        "Réponds en français, bullet points."
    )
    # Sélection de la stratégie
    try:
        strat = model_name.lower()
        if strat == "cascade":
            return cascade_generate(prompt)
        elif strat == "ensemble":
            return ensemble_generate(prompt)
        else:
            # hybride par défaut
            return hybrid_generate_summary(demographics, medications, problems)
    except Exception as exc:  # pragma: no cover
        print(f"[LLM] fallback chaîne – {exc}")
    # Fallback local ou HF
    prompt_hash = str(hash(prompt))
    try:
        resolved = _resolve_name(model_name)
        return _cached_local_generate(resolved, prompt_hash, prompt)
    except Exception:
        return _hf_generate(prompt)
