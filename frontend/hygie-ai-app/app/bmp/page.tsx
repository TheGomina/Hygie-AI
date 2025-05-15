'use client';
import { useState } from 'react';
import PatientForm, { Demographics } from '@/components/PatientForm';
import MedicationList from '@/components/MedicationList';
import ResultPanel from '@/components/ResultPanel';
import LoadingSpinner from '@/components/LoadingSpinner';
import SectionCard from '@/components/SectionCard';
import { Button } from '@/components/Button';
import NProgress from 'nprogress';
import { toast } from 'sonner';

type BmpResult = {
  status: string;
  problems: string[];
  recommendations: string[];
  summary?: string;
};

export default function BmpPage() {
  const [demo, setDemo] = useState<Demographics>({ age: 70, sex: 'F' });
  const [meds, setMeds] = useState<string[]>([]);
  const [result, setResult] = useState<BmpResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function runBmp() {
    setLoading(true);
    NProgress.start();
    setError(null);
    try {
      const payload = {
        patient_id: 'demo1',
        demographics: demo,
        medications: meds.map((m) => ({ name: m })),
      };
      const res = await fetch('/api/bmp', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error('Erreur API');
      const data: BmpResult = await res.json();
      setResult(data);
      toast.success('Analyse terminée');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erreur inconnue';
      setError(msg);
      toast.error(msg);
    } finally {
      setLoading(false);
      NProgress.done();
    }
  }

  return (
    <>
      <h1 className="mb-6 text-3xl font-bold tracking-wide">Analyse BMP</h1>

      <div className="space-y-6">
        <SectionCard title="Données patient">
          <PatientForm value={demo} onChange={setDemo} />
        </SectionCard>

        <SectionCard title="Médicaments">
          <MedicationList value={meds} onChange={setMeds} />
        </SectionCard>

        <div className="flex justify-end">
          <Button
            onClick={runBmp}
            disabled={loading || meds.length === 0}
            className="min-w-[200px]"
          >
            {loading ? 'Analyse en cours…' : "Lancer l'analyse"}
          </Button>
        </div>

        {loading && <LoadingSpinner />}
        {error && (
          <p className="rounded bg-red-100 p-3 text-red-700 dark:bg-red-900/30 dark:text-red-300">
            {error}
          </p>
        )}

        {result && (
          <SectionCard title="Résultats">
            <ResultPanel result={result} />
          </SectionCard>
        )}
      </div>
    </>
  );
}