"use client";
import { useState } from "react";
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select } from '@/components/ui/select';
import { Calendar, User } from 'lucide-react';

export interface Demographics {
  age: number;
  sex: string;
}

interface Props {
  value: Demographics;
  onChange: (val: Demographics) => void;
}

export default function PatientForm({ value, onChange }: Props) {
  const [age, setAge] = useState(value.age ?? 0);
  const [sex, setSex] = useState(value.sex ?? "M");

  function emit() {
    onChange({ age: age || 0, sex });
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <div className="space-y-1">
        <Label htmlFor="age" className="flex items-center gap-1">
          <Calendar className="h-4 w-4 text-primary" /> Âge
        </Label>
        <Input
          id="age"
          type="number"
          min={0}
          max={120}
          value={age}
          onChange={(e) => setAge(parseInt(e.target.value))}
          onBlur={emit}
        />
      </div>
      <div className="space-y-1">
        <Label htmlFor="sex" className="flex items-center gap-1">
          <User className="h-4 w-4 text-primary" /> Sexe
        </Label>
        <Select
          id="sex"
          value={sex}
          onChange={(e) => {
            setSex(e.target.value);
            emit();
          }}
        >
          <option value="M">Homme</option>
          <option value="F">Femme</option>
        </Select>
      </div>
    </div>
  );
}
