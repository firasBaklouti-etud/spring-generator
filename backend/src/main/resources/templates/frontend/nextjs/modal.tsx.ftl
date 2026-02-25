"use client";

import React, { useEffect, useRef } from "react";
import Button from "./button";

interface ModalProps {
  open: boolean;
  title: string;
  message?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: "danger" | "primary";
}

export default function Modal({
  open,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  onConfirm,
  onCancel,
  variant = "primary",
}: ModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    if (open) {
      dialogRef.current?.showModal();
    } else {
      dialogRef.current?.close();
    }
  }, [open]);

  if (!open) return null;

  return (
    <dialog
      ref={dialogRef}
      className="rounded-xl shadow-xl border-0 p-0 backdrop:bg-black/50"
      onClose={onCancel}
    >
      <div className="p-6 min-w-[320px]">
        <h3 className="text-lg font-semibold mb-2">{title}</h3>
        {message && <p className="text-gray-600 text-sm mb-6">{message}</p>}
        <div className="flex justify-end space-x-3">
          <Button variant="secondary" onClick={onCancel}>
            {cancelLabel}
          </Button>
          <Button variant={variant} onClick={onConfirm}>
            {confirmLabel}
          </Button>
        </div>
      </div>
    </dialog>
  );
}
