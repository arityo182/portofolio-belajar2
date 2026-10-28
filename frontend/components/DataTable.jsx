"use client";

import { useEffect, useMemo, useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  Download,
  Inbox,
  Search,
} from "lucide-react";
import { cn, formatNumber } from "@/lib/utils";
import { Button } from "@/components/ui/Button";
import EmptyState from "@/components/ui/EmptyState";

function defaultValue(row, column) {
  if (typeof column.accessor === "function") {
    return column.accessor(row);
  }
  return row[column.key];
}

function compareValues(left, right) {
  if (left === right) {
    return 0;
  }
  if (left === null || left === undefined) {
    return 1;
  }
  if (right === null || right === undefined) {
    return -1;
  }
  const leftNumber = typeof left === "number" ? left : Number(left);
  const rightNumber = typeof right === "number" ? right : Number(right);
  const bothNumeric =
    Number.isFinite(leftNumber) &&
    Number.isFinite(rightNumber) &&
    String(left).trim() !== "" &&
    String(right).trim() !== "";
  if (bothNumeric) {
    return leftNumber - rightNumber;
  }
  const leftDate = Date.parse(left);
  const rightDate = Date.parse(right);
  if (Number.isFinite(leftDate) && Number.isFinite(rightDate)) {
    return leftDate - rightDate;
  }
  return String(left).localeCompare(String(right), "id", { sensitivity: "base" });
}

function csvCell(value) {
  if (value === null || value === undefined) {
    return '""';
  }
  const text = String(value).replace(/"/g, '""');
  return `"${text}"`;
}

export default function DataTable({
  columns = [],
  data = [],
  rowKey = (row, index) => row.id ?? index,
  pageSize = 10,
  searchable = false,
  searchPlaceholder = "Cari...",
  searchKeys = [],
  initialSort = null,
  toolbar = null,
  filters = null,
  emptyTitle = "Belum ada data",
  emptyDescription = "Data akan tampil di sini setelah tersedia.",
  caption = "Tabel data",
  exportFileName,
  selectable = false,
  onSelectionChange,
  loading = false,
  className,
}) {
  const [search, setSearch] = useState("");
  const [sort, setSort] = useState(initialSort);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(new Set());

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query || !searchable) {
      return data;
    }
    return data.filter((row) => {
      const keys = searchKeys.length > 0 ? searchKeys : columns.map((column) => column.key);
      return keys.some((key) => {
        const value = typeof key === "function" ? key(row) : row[key];
        return String(value ?? "").toLowerCase().includes(query);
      });
    });
  }, [columns, data, search, searchable, searchKeys]);

  const sorted = useMemo(() => {
    if (!sort?.key || !sort.direction) {
      return filtered;
    }
    const column = columns.find((item) => item.key === sort.key);
    if (!column) {
      return filtered;
    }
    const direction = sort.direction === "desc" ? -1 : 1;
    return [...filtered].sort(
      (left, right) =>
        direction * compareValues(defaultValue(left, column), defaultValue(right, column)),
    );
  }, [columns, filtered, sort]);

  const totalPages = Math.max(1, Math.ceil(sorted.length / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const pageRows = sorted.slice(safePage * pageSize, safePage * pageSize + pageSize);

  useEffect(() => {
    if (page > totalPages - 1) {
      setPage(Math.max(0, totalPages - 1));
    }
  }, [page, totalPages]);

  useEffect(() => {
    setPage(0);
  }, [search]);

  function toggleSort(column) {
    if (!column.sortable) {
      return;
    }
    setSort((current) => {
      if (!current || current.key !== column.key) {
        return { key: column.key, direction: "asc" };
      }
      if (current.direction === "asc") {
        return { key: column.key, direction: "desc" };
      }
      return null;
    });
    setPage(0);
  }

  function toggleRow(key) {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      onSelectionChange?.(
        sorted.filter((row) => next.has(rowKey(row))),
      );
      return next;
    });
  }

  function togglePage() {
    setSelected((current) => {
      const next = new Set(current);
      const allSelected = pageRows.every((row) => next.has(rowKey(row)));
      for (const row of pageRows) {
        const key = rowKey(row);
        if (allSelected) {
          next.delete(key);
        } else {
          next.add(key);
        }
      }
      onSelectionChange?.(sorted.filter((row) => next.has(rowKey(row))));
      return next;
    });
  }

  function exportCsv() {
    if (!exportFileName) {
      return;
    }
    const exportRows =
      selected.size > 0 ? sorted.filter((row) => selected.has(rowKey(row))) : sorted;
    const exportableColumns = columns.filter((column) => column.exportable !== false);
    const header = exportableColumns.map((column) => csvCell(column.header)).join(",");
    const body = exportRows
      .map((row) =>
        exportableColumns
          .map((column) => {
            const value = column.exportValue
              ? column.exportValue(row)
              : defaultValue(row, column);
            return csvCell(value);
          })
          .join(","),
      )
      .join("\n");
    const blob = new Blob([`\uFEFF${header}\n${body}`], {
      type: "text/csv;charset=utf-8",
    });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = exportFileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  }

  const allPageSelected =
    pageRows.length > 0 && pageRows.every((row) => selected.has(rowKey(row)));

  return (
    <section className={cn("space-y-3", className)} aria-label={caption}>
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-2">{toolbar}</div>
        <div className="flex flex-wrap items-center gap-2">
          {filters}
          {searchable ? (
            <label className="relative block">
              <span className="sr-only">{searchPlaceholder}</span>
              <Search
                className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-content-subtle"
                aria-hidden="true"
              />
              <input
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder={searchPlaceholder}
                className="h-10 w-full rounded-xl border border-line bg-surface-raised pl-9 pr-3 text-sm text-content shadow-sm outline-none placeholder:text-content-subtle focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25 sm:w-64"
              />
            </label>
          ) : null}
          {exportFileName ? (
            <Button
              variant="secondary"
              size="sm"
              onClick={exportCsv}
              leftIcon={<Download className="h-4 w-4" />}
            >
              Export CSV
            </Button>
          ) : null}
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-line bg-surface-raised shadow-card">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] border-collapse text-left">
            <caption className="sr-only">{caption}</caption>
            <thead>
              <tr className="border-b border-line bg-surface-subtle text-2xs uppercase tracking-[0.12em] text-content-subtle">
                {selectable ? (
                  <th scope="col" className="w-10 px-3 py-3">
                    <input
                      type="checkbox"
                      checked={allPageSelected}
                      onChange={togglePage}
                      aria-label="Pilih semua baris pada halaman ini"
                      className="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-2 focus:ring-primary-600 dark:border-neutral-600 dark:bg-neutral-800"
                    />
                  </th>
                ) : null}
                {columns.map((column) => {
                  const ariaSort =
                    sort?.key === column.key
                      ? sort.direction === "asc"
                        ? "ascending"
                        : "descending"
                      : "none";
                  return (
                    <th
                      key={column.key}
                      scope="col"
                      aria-sort={column.sortable ? ariaSort : undefined}
                      className={cn(
                        "px-4 py-3 font-semibold",
                        column.align === "right" && "text-right",
                        column.className,
                      )}
                    >
                      {column.sortable ? (
                        <button
                          type="button"
                          onClick={() => toggleSort(column)}
                          className={cn(
                            "inline-flex items-center gap-1 rounded text-2xs uppercase tracking-[0.12em] transition-colors hover:text-content focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600",
                            column.align === "right" && "flex-row-reverse",
                          )}
                        >
                          {column.header}
                          {sort?.key === column.key ? (
                            sort.direction === "asc" ? (
                              <ArrowUp className="h-3 w-3" aria-hidden="true" />
                            ) : (
                              <ArrowDown className="h-3 w-3" aria-hidden="true" />
                            )
                          ) : (
                            <ArrowUpDown className="h-3 w-3 opacity-50" aria-hidden="true" />
                          )}
                        </button>
                      ) : (
                        column.header
                      )}
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {loading ? (
                Array.from({ length: Math.min(pageSize, 5) }).map((_, index) => (
                  <tr key={`skeleton-${index}`} aria-hidden="true">
                    {selectable ? <td className="px-3 py-4" /> : null}
                    {columns.map((column) => (
                      <td key={column.key} className="px-4 py-4">
                        <div className="skeleton h-4 w-full max-w-[10rem]" />
                      </td>
                    ))}
                  </tr>
                ))
              ) : pageRows.length === 0 ? (
                <tr>
                  <td colSpan={columns.length + (selectable ? 1 : 0)} className="px-4 py-4">
                    <EmptyState icon={Inbox} title={emptyTitle} description={emptyDescription} compact />
                  </td>
                </tr>
              ) : (
                pageRows.map((row) => {
                  const key = rowKey(row);
                  return (
                    <tr key={key} className="transition-colors hover:bg-surface-subtle">
                      {selectable ? (
                        <td className="px-3 py-3.5">
                          <input
                            type="checkbox"
                            checked={selected.has(key)}
                            onChange={() => toggleRow(key)}
                            aria-label="Pilih baris"
                            className="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-2 focus:ring-primary-600 dark:border-neutral-600 dark:bg-neutral-800"
                          />
                        </td>
                      ) : null}
                      {columns.map((column) => (
                        <td
                          key={column.key}
                          className={cn(
                            "px-4 py-3.5 text-sm text-content",
                            column.align === "right" && "text-right",
                            column.cellClassName,
                          )}
                        >
                          {column.render
                            ? column.render(row)
                            : defaultValue(row, column) ?? "—"}
                        </td>
                      ))}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col gap-3 border-t border-line px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-xs text-content-muted">
            {sorted.length === 0
              ? "Tidak ada data"
              : `Menampilkan ${safePage * pageSize + 1}–${Math.min(
                  sorted.length,
                  safePage * pageSize + pageRows.length,
                )} dari ${formatNumber(sorted.length)} data`}
            {selected.size > 0 ? ` · ${selected.size} dipilih` : ""}
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              disabled={safePage <= 0}
              leftIcon={<ChevronLeft className="h-4 w-4" />}
            >
              Sebelumnya
            </Button>
            <span className="numeric px-1 text-xs text-content-muted">
              {safePage + 1} / {totalPages}
            </span>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
              disabled={safePage >= totalPages - 1}
              rightIcon={<ChevronRight className="h-4 w-4" />}
            >
              Berikutnya
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}
