import clsx from "clsx";

export default function Card({ className, children }: { className?: string; children: React.ReactNode }) {
  return (
    <div className={clsx("rounded-xl bg-white shadow-md p-6 dark:bg-gray-900", className)}>
      {children}
    </div>
  );
}
