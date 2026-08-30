import { settingsApi } from '../../shared/api/endpoints'
import type { FeatureFlags } from '../../shared/types/api'

import { useAuthQuery } from '../auth/useAuthQuery'
const COMMUNITY_DEFAULTS: FeatureFlags = {
  planCode: 'community',
  flags: {
    enterprise: false,
    billing: true,
    audit_export: false,
  },
}

/** Installation plan + feature flags for nav/API gating. */
export function useFeatureFlags() {
  const query = useAuthQuery({
    queryKey: ['settings', 'features'],
    queryFn: () => settingsApi.features(),
    enabled: true,
    staleTime: 60_000,
  })

  const data = query.data ?? COMMUNITY_DEFAULTS
  return {
    planCode: data.planCode,
    flags: data.flags,
    isEnterprise: !!data.flags.enterprise,
    billingEnabled: data.flags.billing !== false,
    auditExportEnabled: !!data.flags.audit_export,
    isLoading: query.isLoading,
    isError: query.isError,
  }
}
