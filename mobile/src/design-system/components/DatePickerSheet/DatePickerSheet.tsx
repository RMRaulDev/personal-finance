import { useState } from 'react';
import { Modal, Pressable, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useThemedStyles } from '../../theme';
import { space } from '../../tokens';
import { Button } from '../Button';
import { Text } from '../Text';

import {
  compareDates,
  dayNumber,
  monthBounds,
  monthLongName,
  monthOf,
  monthShortName,
  monthTitle,
  monthWeeks,
  shiftMonth,
  shortDate,
  WEEKDAY_INITIALS,
  weekdayDate,
} from './DatePickerSheet.model';
import { createDatePickerSheetStyles } from './DatePickerSheet.styles';
import { DayCell } from './DayCell';

/** `DatePickerSheet` props. */
export interface DatePickerSheetProps {
  /** Title of the sheet, usually the field's label ("Primer pago"). */
  title: string;
  /** The server's today (ISO), marked in the grid and named in the legend. */
  today: string;
  /** The date chosen when the sheet opens (ISO), or null. */
  value: string | null;
  /** Any date of the month shown first. Defaults to `value`, else `today`. */
  initialMonth?: string;
  /** Earliest date that can be picked (ISO), included. Earlier days are drawn disabled. */
  min?: string;
  /** Latest date that can be picked (ISO), included. Later days are drawn disabled. */
  max?: string;
  /** Names `min` in the legend ("Desde el 9 sep"). Defaults to true. */
  showMinInLegend?: boolean;
  /** Receives the picked ISO date; the caller closes the sheet. */
  onConfirm: (iso: string) => void;
  /** Closes without changing the date: "Cancelar", a tap outside, or Android's Back. */
  onCancel: () => void;
}

/**
 * Month sheet of the date picker: month navigation with text ("‹ sep", "nov ›"; never icon-only),
 * a Monday-first grid of 44px days, a legend that says in words which day is today, which one is
 * chosen, and the allowed range (so color never carries it alone), and "Usar {fecha}". Part of the
 * date picker proposal ("Selector de fecha") from the "Compromisos v1" screen design, pending
 * adoption in the Lucas DS source project. Mount it only while open.
 */
export function DatePickerSheet({
  title,
  today,
  value,
  initialMonth,
  min,
  max,
  showMinInLegend = true,
  onConfirm,
  onCancel,
}: DatePickerSheetProps) {
  const styles = useThemedStyles(createDatePickerSheetStyles);
  const insets = useSafeAreaInsets();
  const [month, setMonth] = useState(() => monthOf(initialMonth ?? value ?? today));
  const [pick, setPick] = useState<string | null>(value);

  const previous = shiftMonth(month, -1);
  const next = shiftMonth(month, 1);
  // A month entirely outside the range cannot be reached.
  const previousDisabled = min !== undefined && compareDates(min, monthBounds(previous).last) < 0;
  const nextDisabled = max !== undefined && compareDates(max, monthBounds(next).first) > 0;

  /** True when a day is outside the allowed range. */
  function outOfRange(iso: string): boolean {
    return (min !== undefined && compareDates(min, iso) < 0) || (max !== undefined && compareDates(max, iso) > 0);
  }

  const legend = [
    `Hoy: ${weekdayDate(today, today)}`,
    pick !== null ? `Elegida: ${weekdayDate(today, pick)}` : null,
    min !== undefined && showMinInLegend ? `Desde el ${shortDate(today, min)}` : null,
    max !== undefined ? (max === today ? 'Hasta hoy' : `Hasta el ${shortDate(today, max)}`) : null,
  ]
    .filter((part) => part !== null)
    .join(' · ');

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
        <View
          accessibilityViewIsModal
          aria-modal
          aria-label={title}
          style={[styles.sheet, { paddingBottom: space[8] + insets.bottom }]}
        >
          <View accessibilityElementsHidden importantForAccessibility="no" style={styles.handle} />
          <Text variant="heading" accessibilityRole="header">
            {title}
          </Text>
          <View style={styles.navigation}>
            <Button
              variant="ghost"
              size="sm"
              inactive={previousDisabled}
              accessibilityLabel={`Mes anterior, ${monthLongName(previous)}`}
              onPress={() => setMonth(previous)}
            >
              {`‹ ${monthShortName(previous)}`}
            </Button>
            <Text variant="subheading" accessibilityLiveRegion="polite">
              {monthTitle(month)}
            </Text>
            <Button
              variant="ghost"
              size="sm"
              inactive={nextDisabled}
              accessibilityLabel={`Mes siguiente, ${monthLongName(next)}`}
              onPress={() => setMonth(next)}
            >
              {`${monthShortName(next)} ›`}
            </Button>
          </View>
          <View role="grid" aria-label={monthTitle(month)} style={styles.grid}>
            <View style={styles.week} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
              {WEEKDAY_INITIALS.map((initial, index) => (
                <View key={index} style={styles.weekdayHeader}>
                  <Text variant="caption" color="secondary">
                    {initial}
                  </Text>
                </View>
              ))}
            </View>
            {monthWeeks(month).map((week, weekIndex) => (
              <View key={weekIndex} style={styles.week}>
                {week.map((iso, dayIndex) => {
                  if (iso === null) {
                    return <View key={`blank-${dayIndex}`} style={styles.blank} />;
                  }
                  const isToday = iso === today;
                  const selected = iso === pick;
                  const disabled = outOfRange(iso);
                  const states = [isToday && 'hoy', selected && 'elegida', disabled && 'no disponible'].filter(Boolean);
                  return (
                    <DayCell
                      key={iso}
                      label={dayNumber(iso)}
                      accessibilityLabel={[weekdayDate(today, iso), ...states].join(', ')}
                      today={isToday}
                      selected={selected}
                      disabled={disabled}
                      onPress={() => setPick(iso)}
                    />
                  );
                })}
              </View>
            ))}
          </View>
          <Text variant="body-sm" color="secondary" tabular>
            {legend}
          </Text>
          <View style={styles.actions}>
            <Button variant="ghost" size="lg" onPress={onCancel}>
              Cancelar
            </Button>
            <View style={styles.primarySlot}>
              <Button
                variant="primary"
                size="lg"
                fullWidth
                inactive={pick === null}
                onPress={() => {
                  if (pick !== null) {
                    onConfirm(pick);
                  }
                }}
              >
                {pick !== null ? `Usar ${shortDate(today, pick)}` : 'Elige un día'}
              </Button>
            </View>
          </View>
        </View>
      </View>
    </Modal>
  );
}
