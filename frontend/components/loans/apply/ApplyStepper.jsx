import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

const STEPS = [
  { id: 1, title: "Produk", description: "Pilih produk" },
  { id: 2, title: "Detail", description: "Atur nominal" },
  { id: 3, title: "Dokumen", description: "KTP & selfie" },
  { id: 4, title: "Review", description: "Periksa data" },
];

export default function ApplyStepper({ currentStep = 1 }) {
  return (
    <nav aria-label="Tahapan pengajuan pinjaman">
      <ol className="flex items-start gap-1 sm:gap-2">
        {STEPS.map((step, index) => {
          const active = currentStep === step.id;
          const complete = currentStep > step.id;
          return (
            <li key={step.id} className="flex flex-1 items-start gap-1 sm:gap-2">
              <div
                className="flex min-w-0 flex-1 flex-col items-center text-center sm:flex-row sm:items-center sm:text-left"
                aria-current={active ? "step" : undefined}
              >
                <span
                  className={cn(
                    "flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-xs font-semibold transition-colors",
                    active
                      ? "border-primary-600 bg-primary-600 text-white"
                      : complete
                        ? "border-success-600 bg-success-600 text-white"
                        : "border-line bg-surface-raised text-content-subtle",
                  )}
                  aria-hidden="true"
                >
                  {complete ? <Check className="h-4 w-4" /> : step.id}
                </span>
                <span className="mt-1.5 min-w-0 sm:ml-2 sm:mt-0">
                  <span
                    className={cn(
                      "block truncate text-2xs font-semibold sm:text-xs",
                      active ? "text-content" : "text-content-muted",
                    )}
                  >
                    {step.title}
                  </span>
                  <span className="hidden truncate text-2xs text-content-subtle sm:block">
                    {step.description}
                  </span>
                </span>
              </div>
              {index < STEPS.length - 1 ? (
                <span
                  className={cn(
                    "mt-4 h-px min-w-3 flex-1 sm:min-w-6",
                    complete ? "bg-success-400" : "bg-line",
                  )}
                  aria-hidden="true"
                />
              ) : null}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
