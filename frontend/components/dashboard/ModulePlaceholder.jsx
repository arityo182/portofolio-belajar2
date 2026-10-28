import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import EmptyState from "@/components/ui/EmptyState";

export default function ModulePlaceholder({
  eyebrow,
  title,
  description,
  emptyTitle,
  emptyDescription,
  icon,
  action,
}) {
  return (
    <div className="space-y-6">
      <PageHeader eyebrow={eyebrow} title={title} description={description} />

      <Card>
        <CardContent className="py-6">
          <EmptyState
            icon={icon}
            title={emptyTitle}
            description={emptyDescription}
            action={action}
          />
        </CardContent>
      </Card>
    </div>
  );
}
