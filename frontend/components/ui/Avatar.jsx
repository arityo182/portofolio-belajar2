import { cn, getInitials } from "@/lib/utils";

const SIZES = {
  sm: "h-8 w-8 text-xs",
  md: "h-10 w-10 text-sm",
  lg: "h-14 w-14 text-lg",
};

export default function Avatar({ name, email, src, size = "md", className }) {
  const label = name || email || "Pengguna";
  const initials = getInitials(label);

  return (
    <span
      className={cn(
        "relative inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full border border-primary-200 bg-primary-50 font-semibold text-primary-700 dark:border-primary-800 dark:bg-primary-950/60 dark:text-primary-300",
        SIZES[size] || SIZES.md,
        className,
      )}
      aria-hidden="true"
    >
      {src ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={src} alt="" className="h-full w-full object-cover" />
      ) : (
        initials
      )}
    </span>
  );
}
