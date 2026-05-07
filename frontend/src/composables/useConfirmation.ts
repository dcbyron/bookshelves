import { useDialog } from "naive-ui";

export interface ConfirmationOptions {
  title: string;
  message: string;
  positiveText?: string;
  negativeText?: string;
}

export function useConfirmation() {
  const dialog = useDialog();

  function confirm(options: ConfirmationOptions): Promise<boolean> {
    return new Promise((resolve) => {
      let settled = false;
      const finish = (value: boolean) => {
        if (!settled) {
          settled = true;
          resolve(value);
        }
      };
      const instance = dialog.warning({
        title: options.title,
        content: options.message,
        positiveText: options.positiveText ?? "Confirm",
        negativeText: options.negativeText ?? "Cancel",
        onPositiveClick: () => {
          finish(true);
          instance.destroy();
        },
        onNegativeClick: () => {
          finish(false);
          instance.destroy();
        },
        onClose: () => {
          finish(false);
        }
      });
    });
  }

  return { confirm };
}
