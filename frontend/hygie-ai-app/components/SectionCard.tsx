import Card from "@/components/Card";

export default function SectionCard({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <Card className="space-y-4">
      <h2 className="text-xl font-semibold">{title}</h2>
      {children}
    </Card>
  );
}
