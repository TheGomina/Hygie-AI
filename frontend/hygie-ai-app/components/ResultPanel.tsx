"use client";
import { useState } from "react";

interface Props {
  result: any | null;
}

export default function ResultPanel({ result }: Props) {
  if (!result) return null;

  const [open, setOpen] = useState<string | null>(null);

  function Section({ id, title, content }: { id: string; title: string; content: string[] }) {
    const isOpen = open === id;
    return (
      <div className="rounded border">
        <button
          type="button"
          className="flex w-full justify-between bg-gray-100 p-3 text-lg"
          onClick={() => setOpen(isOpen ? null : id)}
        >
          {title}
          <span>{isOpen ? "-" : "+"}</span>
        </button>
        {isOpen && (
          <ul className="space-y-1 p-4 list-disc list-inside">
            {content.map((c) => (
              <li key={c}>{c}</li>
            ))}
          </ul>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <Section id="probl" title="Problèmes" content={result.problems ?? []} />
      <Section id="recom" title="Recommandations" content={result.recommendations ?? []} />
    </div>
  );
}
