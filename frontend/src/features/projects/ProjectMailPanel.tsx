import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { projectMailApi } from '../../shared/api/endpoints'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { QueryState } from '../../shared/components/QueryState'
import { StatusChip } from '../../shared/components/StatusChip'
import {
  MAIL_API_TOKEN_SECRET,
} from '../secrets/knownSecretHints'

export function ProjectMailPanel({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient()
  const [to, setTo] = useState('')
  const [subject, setSubject] = useState('')
  const [textBody, setTextBody] = useState('')
  const [sendResult, setSendResult] = useState<string | null>(null)

  const statusQuery = useQuery({
    queryKey: ['projects', projectId, 'mail'],
    queryFn: () => projectMailApi.status(projectId),
    enabled: !!projectId,
  })

  const provisionMutation = useMutation({
    mutationFn: () => projectMailApi.provision(projectId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'mail'] })
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const sendMutation = useMutation({
    mutationFn: () =>
      projectMailApi.send(projectId, {
        to,
        subject,
        textBody,
      }),
    onSuccess: (result) => {
      setSendResult(result.detail)
      void queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'mail'] })
    },
  })

  const data = statusQuery.data

  return (
    <DetailPanel>
      <Stack spacing={1.5}>
        <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
          Mail
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Own platform SMTP (Postfix). On each deploy Atlas auto-provisions secrets and writes{' '}
          <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
            SMTP_*
          </Typography>{' '}
          into the app{' '}
          <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
            .env
          </Typography>
          . Manual provision still works for rotate.
        </Typography>

        <QueryState isLoading={statusQuery.isLoading} isError={statusQuery.isError}>
          {data && (
            <Stack spacing={1.5}>
              <DetailField label="Status">
                <StatusChip
                  label={
                    data.provisioned
                      ? 'PROVISIONED'
                      : data.provisionerConfigured
                        ? 'READY'
                        : 'NOT_CONFIGURED'
                  }
                />
              </DetailField>
              <DetailField label="From" mono>
                {data.from}
              </DetailField>
              {data.host && (
                <DetailField label="Relay" mono>
                  {data.host}:{data.port}
                </DetailField>
              )}
              {data.provisioned && (
                <DetailField label="Sends today">
                  {data.dailySendLimit - data.remainingSendsToday} / {data.dailySendLimit}
                </DetailField>
              )}
              <Alert
                severity={data.provisioned ? 'success' : data.provisionerConfigured ? 'info' : 'warning'}
                variant="outlined"
              >
                {data.message}
              </Alert>
              {provisionMutation.isError && (
                <Alert severity="error" variant="outlined">
                  {(() => {
                    const apiMessage =
                      (provisionMutation.error as { response?: { data?: { message?: string } } })
                        ?.response?.data?.message
                    return apiMessage?.trim()
                      ? apiMessage
                      : 'Provision failed. Check ATLAS_APP_SMTP_HOST on the Atlas install.'
                  })()}
                </Alert>
              )}
              {provisionMutation.isSuccess && (
                <Alert severity="success" variant="outlined">
                  {provisionMutation.data.rotated
                    ? 'Rotated SMTP credentials and updated smtp.* secrets.'
                    : 'SMTP credentials stored; redeploy to inject env vars.'}
                </Alert>
              )}
              <Button
                variant="contained"
                disabled={!data.provisionerConfigured || provisionMutation.isPending}
                onClick={() => provisionMutation.mutate()}
                sx={{ alignSelf: 'flex-start' }}
              >
                {data.provisioned ? 'Rotate / re-provision' : 'Provision mail'}
              </Button>

              {data.provisioned && (
                <Stack spacing={1.5} sx={{ pt: 1 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 650 }}>
                    Send test email
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Uses platform relay. Apps should use SMTP env vars or call this API with{' '}
                    <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
                      {MAIL_API_TOKEN_SECRET}
                    </Typography>{' '}
                    when required.
                  </Typography>
                  <TextField
                    size="small"
                    label="To"
                    value={to}
                    onChange={(e) => setTo(e.target.value)}
                    fullWidth
                  />
                  <TextField
                    size="small"
                    label="Subject"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    fullWidth
                  />
                  <TextField
                    size="small"
                    label="Body"
                    value={textBody}
                    onChange={(e) => setTextBody(e.target.value)}
                    multiline
                    minRows={3}
                    fullWidth
                  />
                  <Button
                    variant="outlined"
                    disabled={!to || !subject || !textBody || sendMutation.isPending}
                    onClick={() => sendMutation.mutate()}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    Send
                  </Button>
                  {sendMutation.isError && (
                    <Alert severity="error" variant="outlined">
                      {(() => {
                        const apiMessage =
                          (sendMutation.error as { response?: { data?: { message?: string } } })
                            ?.response?.data?.message
                        return apiMessage?.trim() ? apiMessage : 'Send failed.'
                      })()}
                    </Alert>
                  )}
                  {sendResult && sendMutation.isSuccess && (
                    <Alert severity="success" variant="outlined" onClose={() => setSendResult(null)}>
                      {sendResult} ({sendMutation.data.remainingToday} sends remaining today)
                    </Alert>
                  )}
                </Stack>
              )}
            </Stack>
          )}
        </QueryState>
      </Stack>
    </DetailPanel>
  )
}
