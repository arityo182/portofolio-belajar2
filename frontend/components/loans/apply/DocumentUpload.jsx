"use client";

import { useEffect, useRef, useState } from "react";
import { FileImage, Trash2, UploadCloud } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatFileSize } from "@/lib/validations/loan";

export default function DocumentUpload({
  id,
  label,
  description,
  file,
  onFileChange,
  onRemove,
  error,
  required = false,
}) {
  const inputRef = useRef(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [dragging, setDragging] = useState(false);

  useEffect(() => {
    if (!file) {
      setPreviewUrl(null);
      return undefined;
    }
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  function handleFiles(fileList) {
    const nextFile = fileList?.[0];
    if (nextFile) {
      onFileChange(nextFile);
    }
  }

  return (
    <div>
      <div className="mb-1.5 flex items-baseline justify-between gap-3">
        <label htmlFor={id} className="block text-sm font-medium text-content">
          {label}
          {required ? (
            <>
              <span className="ml-0.5 text-danger-600" aria-hidden="true">
                *
              </span>
              <span className="sr-only"> (wajib)</span>
            </>
          ) : null}
        </label>
        <span className="text-2xs text-content-subtle">Maks. 5 MB</span>
      </div>

      {file && previewUrl ? (
        <div className="flex gap-3 rounded-xl border border-line bg-surface-raised p-3">
          <span className="relative h-20 w-20 shrink-0 overflow-hidden rounded-lg border border-line bg-surface-subtle">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={previewUrl} alt={`Pratinjau ${label}`} className="h-full w-full object-cover" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="flex items-center gap-1.5 truncate text-sm font-medium text-content">
              <FileImage className="h-4 w-4 shrink-0 text-content-subtle" aria-hidden="true" />
              {file.name}
            </p>
            <p className="numeric mt-1 text-2xs text-content-subtle">{formatFileSize(file.size)}</p>
            <div className="mt-2 flex gap-3">
              <button
                type="button"
                onClick={() => inputRef.current?.click()}
                className="rounded text-xs font-semibold text-primary-700 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:text-primary-300"
              >
                Ganti berkas
              </button>
              <button
                type="button"
                onClick={() => {
                  onRemove?.();
                  if (inputRef.current) {
                    inputRef.current.value = "";
                  }
                }}
                className="inline-flex items-center gap-1 rounded text-xs font-semibold text-danger-600 underline-offset-2 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-danger-600 dark:text-danger-300"
              >
                <Trash2 className="h-3 w-3" aria-hidden="true" />
                Hapus
              </button>
            </div>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          onDragOver={(event) => {
            event.preventDefault();
            setDragging(true);
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(event) => {
            event.preventDefault();
            setDragging(false);
            handleFiles(event.dataTransfer.files);
          }}
          className={cn(
            "flex w-full flex-col items-center justify-center rounded-xl border border-dashed px-4 py-7 text-center transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 focus-visible:ring-offset-2 focus-visible:ring-offset-surface",
            dragging
              ? "border-primary-500 bg-primary-50 dark:bg-primary-950/40"
              : "border-line bg-surface-raised hover:border-primary-300 hover:bg-surface-subtle",
            error ? "border-danger-500" : "",
          )}
        >
          <UploadCloud className="h-6 w-6 text-content-subtle" aria-hidden="true" />
          <span className="mt-2 text-sm font-semibold text-content">Pilih atau seret berkas</span>
          <span className="mt-1 text-xs text-content-muted">{description || "JPEG atau PNG"}</span>
        </button>
      )}

      <input
        ref={inputRef}
        id={id}
        type="file"
        accept="image/jpeg,image/png"
        className="sr-only"
        onChange={(event) => handleFiles(event.target.files)}
      />

      {error ? (
        <p role="alert" className="mt-1.5 text-xs text-danger-600 dark:text-danger-300">
          {error}
        </p>
      ) : (
        <p className="mt-1.5 text-xs leading-5 text-content-muted">
          Pratinjau lokal. Dokumen tidak dikirim ke server pada tahap ini.
        </p>
      )}
    </div>
  );
}
