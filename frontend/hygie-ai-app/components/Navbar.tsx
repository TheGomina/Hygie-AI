"use client";
import { useEffect, useState } from "react";
import { Button } from "@/components/Button";
import clsx from "clsx";

export default function Navbar() {
  const [dark, setDark] = useState(false);

  useEffect(() => {
    document.documentElement.classList.toggle("dark", dark);
  }, [dark]);

  return (
    <header className="sticky top-0 z-20 w-full bg-white/90 py-2 backdrop-blur dark:bg-gray-900/90">
      <div className="container mx-auto flex max-w-5xl items-center justify-between px-4">
        <h1 className="text-lg font-bold tracking-wide">Hygie-AI</h1>
        <Button
          size="sm"
          variant="ghost"
          className={clsx("", dark && "text-yellow-400")}
          onClick={() => setDark((v) => !v)}
        >
          {dark ? "☀️" : "🌙"}
        </Button>
      </div>
    </header>
  );
}
