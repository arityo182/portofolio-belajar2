export function Logo() {
  return (
    <span className="flex items-center gap-2.5">
      <span
        aria-hidden="true"
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-blue-600"
      >
        <span className="text-base font-black text-white">K</span>
      </span>
      <span className="flex flex-col">
        <span className="text-base font-bold leading-none text-gray-900">
          Koperasi<span className="text-blue-600">Digital</span>
        </span>
        <span className="mt-0.5 text-[10px] leading-none text-gray-400">P2P Lending</span>
      </span>
    </span>
  );
}
