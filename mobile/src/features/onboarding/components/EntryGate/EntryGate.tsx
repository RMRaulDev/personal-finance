import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';

import { accountsQuery } from '../../../../api';
import { errorMessage, ScreenError, ScreenLoading } from '../../../../shared';

import { needsOnboarding } from '../../onboarding.model';

/** Props of `EntryGate`. */
export interface EntryGateProps {
  /** Rendered when the user has no account yet (usually a redirect to the onboarding). */
  onboarding: ReactNode;
  /** Rendered when the user already has an account (usually a redirect to the dashboard). */
  home: ReactNode;
}

/**
 * Decides where the app opens: the onboarding when the user has no account, the home screen
 * otherwise. Shows the loading screen while the accounts load and the error screen, with a retry,
 * if they fail. The route decides how to get there.
 */
export function EntryGate({ onboarding, home }: EntryGateProps) {
  const gate = useQuery({ ...accountsQuery(), select: needsOnboarding });

  if (gate.isPending) {
    return <ScreenLoading />;
  }
  if (gate.data === undefined) {
    return <ScreenError message={errorMessage(gate.error)} onRetry={() => void gate.refetch()} />;
  }
  return <>{gate.data ? onboarding : home}</>;
}
