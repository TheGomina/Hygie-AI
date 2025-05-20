import pytest
import prometheus_client
from pathlib import Path
from backend.bmp_service import anticholinergic, bdpm_loader, interactions_ref, llm_orchestrator


def get_histogram_count(stage):
    for metric in prometheus_client.REGISTRY.collect():
        if metric.name == 'hygie_llm_orchestration_seconds':
            for sample in metric.samples:
                if sample.name.endswith('_count') and sample.labels.get('stage') == stage:
                    return sample.value
    return 0


def test_hybrid_metrics(monkeypatch):
    # Stub ensemble_generate for speed
    monkeypatch.setattr(llm_orchestrator, 'ensemble_generate', lambda p: 'out')
    before = get_histogram_count('hybrid_generate_summary')
    llm_orchestrator.hybrid_generate_summary({'age': 0, 'sex': 'U'}, [], [])
    after = get_histogram_count('hybrid_generate_summary')
    assert after == before + 1


def test_cascade_metrics(monkeypatch):
    # Stub local and HF generates
    monkeypatch.setattr(llm_orchestrator, '_cached_local_generate', lambda name, h, p: 'out')
    monkeypatch.setattr(llm_orchestrator, '_hf_generate', lambda p: 'out')
    before = get_histogram_count('cascade_generate')
    llm_orchestrator.cascade_generate('prompt')
    after = get_histogram_count('cascade_generate')
    assert after == before + 1


def test_compute_burden_with_mocked_scores(monkeypatch):
    monkeypatch.setattr(anticholinergic, '_load_scores', lambda: {'A': 1, 'B': 2})
    assert anticholinergic.compute_burden_score(['a', 'b', 'c']) == 3


def test_load_scores_from_csv(tmp_path, monkeypatch):
    csv_file = tmp_path / "CIA-ACB-test.csv"
    csv_file.write_text(
        "Drug;cia;X;acb\n"
        "DRUG1;1;X;2\n"
        ";;X;\n"
        "DRUG2;;X;3\n"
        "DRUG3;2;X;\n",
        encoding="utf-8",
    )
    monkeypatch.setattr(anticholinergic, '_resources_dir', lambda: tmp_path)
    scores = anticholinergic._load_scores()
    assert scores['DRUG1'] == 2
    assert scores['DRUG2'] == 3
    assert scores['DRUG3'] == 2


def test_get_mapping_fallback(monkeypatch, tmp_path):
    # Simulate missing DB and CSV
    db = tmp_path / "nodb.sqlite"
    csv_file = tmp_path / "no.csv"
    monkeypatch.setattr(bdpm_loader, "DB_PATH", db)
    monkeypatch.setattr(bdpm_loader, "CSV_PATH", csv_file)
    bdpm_loader.get_mapping.cache_clear()
    mapping = bdpm_loader.get_mapping()
    assert mapping == bdpm_loader._FALLBACK


def test_normalize_drug_fallback():
    assert bdpm_loader.normalize_drug('3400932716455') == 'PARACETAMOL'
    assert bdpm_loader.normalize_drug('unknown') == 'UNKNOWN'


def test_get_mapping_sqlite_precedence(monkeypatch, tmp_path):
    # Simulate existing DB path and custom parse_sqlite
    db = tmp_path / "db.sqlite"
    db.write_text("", encoding="utf-8")
    monkeypatch.setattr(bdpm_loader, "DB_PATH", db)
    monkeypatch.setattr(bdpm_loader, "_parse_sqlite", lambda path: {'X': 'Y'})
    bdpm_loader.get_mapping.cache_clear()
    mapping = bdpm_loader.get_mapping()
    assert mapping == {'X': 'Y'}


def test_order():
    assert interactions_ref._order('b', 'a') == ('A', 'B')
    assert interactions_ref._order('A', 'B') == ('A', 'B')


def test_load_from_csv(tmp_path):
    fp = tmp_path / 'ints.csv'
    fp.write_text("A;B;C\nC;D;D\nE;F;A\n", encoding='utf-8')
    mapping = interactions_ref._load_from_csv(fp)
    expected = {('A', 'B'): 'Interaction ANSM (C)', ('C', 'D'): 'Interaction ANSM (D)'}
    assert mapping == expected


def test_load_from_pdf(monkeypatch):
    monkeypatch.setattr(interactions_ref, 'extract_text', lambda f: "G / H\n")
    mapping = interactions_ref._load_from_pdf(Path('dummy.pdf'))
    assert mapping == {('G', 'H'): 'Interaction ANSM (gravité non classée)'}


def test_get_interactions_csv(tmp_path, monkeypatch):
    # CSV precedence
    monkeypatch.setattr(interactions_ref, '_RES', tmp_path)
    interactions_ref.get_interactions.cache_clear()
    csv = tmp_path / 'interaction.csv'
    csv.write_text("A;B;C\n", encoding='utf-8')
    mapping = interactions_ref.get_interactions()
    assert mapping == {('A', 'B'): 'Interaction ANSM (C)'}


def test_get_interactions_pdf(tmp_path, monkeypatch):
    # PDF fallback
    monkeypatch.setattr(interactions_ref, '_RES', tmp_path)
    interactions_ref.get_interactions.cache_clear()
    pdf = tmp_path / 'interaction.pdf'
    pdf.write_text("", encoding='utf-8')
    monkeypatch.setattr(interactions_ref, 'extract_text', lambda f: "X / Y\nZ")
    mapping = interactions_ref.get_interactions()
    assert mapping == {('X', 'Y'): 'Interaction ANSM (gravité non classée)'}

@pytest.mark.benchmark(group="llm_pipeline")
def test_hybrid_pipeline_benchmark(benchmark, monkeypatch):
    monkeypatch.setattr(llm_orchestrator, 'ensemble_generate', lambda p: 'out')
    demo, meds, probs = {'age': 30, 'sex': 'F'}, ['MED'], ['P']
    benchmark(lambda: llm_orchestrator.hybrid_generate_summary(demo, meds, probs))
