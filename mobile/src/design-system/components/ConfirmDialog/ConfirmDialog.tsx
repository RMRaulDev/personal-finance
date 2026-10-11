import { Modal, Pressable, View } from 'react-native';

import { useThemedStyles } from '../../theme';
import { Button } from '../Button';
import { Text } from '../Text';

import { createConfirmDialogStyles } from './ConfirmDialog.styles';

/** `ConfirmDialog` props. */
export interface ConfirmDialogProps {
  /** A question with the name of what it affects, e.g. "¿Archivar Colegiatura?". */
  title: string;
  /** The consequence, in one or two sentences. */
  body: string;
  /** Optional note: what is kept, or whether it can be undone. */
  note?: string;
  /** The exact verb of the action ("Archivar", "Saltar 2 pagos"). It is the dialog's only primary. */
  confirmLabel: string;
  onConfirm: () => void;
  /** Closes without acting: "Cancelar", a tap outside, or Android's Back. */
  onCancel: () => void;
  /**
   * Shows the ghost "Cancelar" button. Defaults to true; false for a dialog that only informs and
   * has a single way out (e.g. "Entendido"), where a tap outside or Back still closes it.
   */
  showCancel?: boolean;
}

/**
 * Confirmation for an action that cannot be undone or that affects several things at once, over the
 * scrim: a question, its consequence, an optional note, "Cancelar" (ghost), and the primary verb.
 * There is no destructive button yet, so it never uses red. Proposal ("Diálogo de confirmación")
 * from the "Compromisos v1" screen design, pending adoption in the Lucas DS source project.
 */
export function ConfirmDialog({
  title,
  body,
  note,
  confirmLabel,
  onConfirm,
  onCancel,
  showCancel = true,
}: ConfirmDialogProps) {
  const styles = useThemedStyles(createConfirmDialogStyles);

  return (
    <Modal
      visible
      transparent
      animationType="fade"
      onRequestClose={onCancel}
      // Android: draw under the status and navigation bars, so the backdrop covers the whole screen.
      statusBarTranslucent
      navigationBarTranslucent
    >
      <View style={styles.container}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Cancelar"
          onPress={onCancel}
          style={styles.backdrop}
        />
        <View role="alertdialog" aria-modal accessibilityViewIsModal aria-label={title} style={styles.dialog}>
          <Text variant="heading" accessibilityRole="header">
            {title}
          </Text>
          <Text color="secondary">{body}</Text>
          {note !== undefined && (
            <Text variant="body-sm" tabular>
              {note}
            </Text>
          )}
          <View style={styles.actions}>
            {showCancel && (
              <Button variant="ghost" size="lg" onPress={onCancel}>
                Cancelar
              </Button>
            )}
            <View style={styles.primarySlot}>
              <Button variant="primary" size="lg" fullWidth onPress={onConfirm}>
                {confirmLabel}
              </Button>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}
