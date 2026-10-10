import { useEffect, useState } from 'react';
import { BackHandler, KeyboardAvoidingView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';

import { useCompleteInitialSetup } from '../../api';
import { AmountField, Button, Field, Root, useThemedStyles } from '../../design-system';
import { formatAmountInput, parseAmountInput } from '../../lib';
import { useMutationLock } from '../../shared';

import { OnboardingStep } from './components/OnboardingStep';
import { PreparationView, usePreparation } from './components/PreparationView';
import { SetupErrorView } from './components/SetupErrorView';
import { createOnboardingScreenStyles } from './OnboardingScreen.styles';
import {
  ACCOUNT_NAME_MAX_LENGTH,
  balanceFeedback,
  balanceTitle,
  DEFAULT_ACCOUNT_NAME,
  NAME_EMPTY_ERROR,
} from './onboarding.model';

/** Props of `OnboardingScreen`. */
export interface OnboardingScreenProps {
  /** Called once the account exists, with its trimmed name; the route moves on to the home screen. */
  onDone: (accountName: string) => void;
}

/**
 * A failed setup with the values it sent, so the summary and "Reintentar" use exactly what was
 * sent, not whatever the fields hold now. Boxed, so a thrown `undefined` still counts as a failure.
 */
interface SetupFailure {
  error: unknown;
  accountName: string;
  openingBalanceCents: number;
}

/**
 * First run, when the user has no account: step 1 names the account, step 2 asks for today's
 * balance, then one setup call creates the account and the default categories. Shows a
 * preparation screen if the call is slow and an error screen, with the typed values kept, if it
 * fails.
 */
export function OnboardingScreen({ onDone }: OnboardingScreenProps) {
  const styles = useThemedStyles(createOnboardingScreenStyles);
  const insets = useSafeAreaInsets();
  const setup = useCompleteInitialSetup();
  // The setup is a mutation like any other: one at a time, and its buttons inactive meanwhile.
  const lock = useMutationLock();
  const preparation = usePreparation();
  const [step, setStep] = useState<1 | 2>(1);
  const [name, setName] = useState(DEFAULT_ACCOUNT_NAME);
  const [nameError, setNameError] = useState<string | undefined>(undefined);
  const [amount, setAmount] = useState('');
  const [emptySubmitted, setEmptySubmitted] = useState(false);
  const [failure, setFailure] = useState<SetupFailure | null>(null);

  const accountName = name.trim();
  const parsedAmount = parseAmountInput(amount);
  const busy = lock.locked || preparation.visible;

  /** Back from step 2 or from the error screen: everything typed in both steps is kept. */
  function editBalance() {
    setFailure(null);
    setStep(2);
  }

  useEffect(() => {
    // Android's Back follows the flow instead of leaving it: it does nothing while the setup runs,
    // returns to step 2 from the error screen and to step 1 from step 2, and closes the app only
    // from step 1, the first screen.
    const subscription = BackHandler.addEventListener('hardwareBackPress', () => {
      if (busy) {
        return true;
      }
      if (failure) {
        // Same as "Editar datos" (`editBalance`), with the stable setters only.
        setFailure(null);
        setStep(2);
        return true;
      }
      if (step === 2) {
        setStep(1);
        return true;
      }
      return false;
    });
    return () => subscription.remove();
  }, [busy, failure, step]);

  /** Step 1 "Continuar": a name made only of spaces counts as empty. */
  function continueToBalance() {
    if (accountName === '') {
      setNameError(NAME_EMPTY_ERROR);
      return;
    }
    setStep(2);
  }

  /**
   * Sends the setup with the given values; a 409 "already completed" counts as done (see `api`).
   * The values are captured here, so a failure remembers exactly what was sent.
   */
  function runSetup(sentName: string, openingBalanceCents: number) {
    lock.run((release) => {
      preparation.begin();
      setup.mutate(
        { accountName: sentName, openingBalanceCents },
        {
          onSuccess: () => preparation.end(() => onDone(sentName)),
          onError: (error) =>
            preparation.end(() => {
              preparation.hide();
              setFailure({ error, accountName: sentName, openingBalanceCents });
            }),
          onSettled: release,
        },
      );
    });
  }

  /** Step 2 "Empezar": always active; it explains what is missing instead. */
  function start() {
    if (parsedAmount.kind === 'empty') {
      setEmptySubmitted(true);
      return;
    }
    // A negative balance already shows its error while typing.
    if (parsedAmount.kind === 'ok') {
      runSetup(accountName, parsedAmount.cents);
    }
  }

  /** The current screen of the flow: preparation, error, or one of the two steps. */
  function content() {
    if (preparation.visible) {
      return <PreparationView accountName={accountName} />;
    }

    if (failure) {
      return (
        <SetupErrorView
          error={failure.error}
          accountName={failure.accountName}
          openingBalanceCents={failure.openingBalanceCents}
          pending={lock.locked}
          onRetry={() => runSetup(failure.accountName, failure.openingBalanceCents)}
          onEdit={editBalance}
        />
      );
    }

    if (step === 1) {
      return (
        <OnboardingStep
          step={1}
          title="¿Dónde tienes tu dinero?"
          text="Empieza con una cuenta: la de nómina, tu débito o tu cartera. Así sabremos de dónde sale cada gasto."
          footer="relaxed"
          action={
            <Button variant="primary" size="lg" fullWidth onPress={continueToBalance}>
              Continuar
            </Button>
          }
        >
          {/* No autofocus: the keyboard would cover the explanation. */}
          <Field
            label="Nombre de la cuenta"
            value={name}
            onChangeText={(text) => {
              setName(text);
              setNameError(undefined);
            }}
            placeholder={DEFAULT_ACCOUNT_NAME}
            maxLength={ACCOUNT_NAME_MAX_LENGTH}
            autoCapitalize="sentences"
            editable={!busy}
            size="lg"
            fullWidth
            help="Solo tú lo ves. Puedes cambiarlo o agregar más cuentas después."
            error={nameError}
          />
        </OnboardingStep>
      );
    }

    const feedback = balanceFeedback(parsedAmount, emptySubmitted);
    return (
      <OnboardingStep
        step={2}
        onBack={() => {
          // Ignored while the setup runs, before the preparation screen appears.
          if (!busy) {
            setStep(1);
          }
        }}
        title={balanceTitle(accountName)}
        text="Es tu punto de partida, no un ingreso. Escribe lo que tienes en esta cuenta ahora."
        footer="keyboard"
        action={
          <Button variant="primary" size="lg" fullWidth inactive={lock.locked} onPress={start}>
            Empezar
          </Button>
        }
      >
        <AmountField
          label="Saldo de hoy"
          value={amount}
          onChangeText={(text) => {
            setAmount(formatAmountInput(text));
            setEmptySubmitted(false);
          }}
          autoFocus
          // What is being sent must not change under the request; the error screen shows it as sent.
          editable={!busy}
          help={feedback.help}
          error={feedback.error}
        />
      </OnboardingStep>
    );
  }

  return (
    <Root style={{ paddingTop: insets.top }}>
      <StatusBar style="auto" />
      {/* "padding" on both platforms: the view adds only the keyboard's overlap, so it is a no-op
          where the system already resized the window. */}
      <KeyboardAvoidingView behavior="padding" style={styles.keyboardArea}>
        {content()}
      </KeyboardAvoidingView>
    </Root>
  );
}
