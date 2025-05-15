"use client";
import { useState } from "react";
import { Input } from '@/components/ui/input';
import { Button } from '@/components/Button';
import { Pill, X } from 'lucide-react';
import { toast } from 'sonner';

interface Props {
  value: string[];
  onChange: (val: string[]) => void;
}

export default function MedicationList({ value, onChange }: Props) {
  const [current, setCurrent] = useState("");

  function add() {
    const name = current.trim();
    if (name && !value.includes(name)) {
      onChange([...value, name]);
      toast.success(`Ajouté : ${name}`);
    }
    setCurrent("");
  }

  function remove(idx: number) {
    onChange(value.filter((_, i) => i !== idx));
  }

  return (
    <div className="space-y-2">
      <div className="flex gap-2">
        <Input
          className="flex-1"
          placeholder="Ajouter médicament (DCI)"
          value={current}
          onChange={(e) => setCurrent(e.target.value.toUpperCase())}
          onKeyDown={(e) => e.key === "Enter" && add()}
        />
        <Button type="button" onClick={add} size="sm">
          Ajouter
        </Button>
      </div>
      <ul className="flex flex-wrap gap-2">
        {value.map((m, i) => (
          <li
            key={m}
            className="flex items-center gap-1 rounded-full bg-primary/10 px-3 py-1 text-sm text-primary dark:bg-primary/20"
          >
            <Pill className="h-3 w-3" /> {m}
            <button onClick={() => remove(i)} className="ml-1 hover:text-red-500">
              <X className="h-3 w-3" />
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
