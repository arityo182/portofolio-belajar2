import { cn } from "@/lib/utils";

export function SectionHeading({ eyebrow, title, description, align = "left" }) {
  return (
    <div
      className={cn(
        "flex flex-col gap-4",
        align === "center" && "mx-auto max-w-2xl items-center text-center",
      )}
    >
      <p className="text-sm font-semibold uppercase tracking-wider text-landing-primary">
        {eyebrow}
      </p>
      <h2 className="text-balance text-3xl font-extrabold tracking-tight text-landing-navy md:text-4xl">
        {title}
      </h2>
      {description ? (
        <p className="text-pretty text-lg leading-relaxed text-landing-body">{description}</p>
      ) : null}
    </div>
  );
}
