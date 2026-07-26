import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { membershipsApi, usersApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { QueryState } from '../../shared/components/QueryState'
import { StatusChip } from '../../shared/components/StatusChip'
import { useAuth } from '../auth/AuthContext'

export function ProjectMembersPanel({ projectId }: { projectId: string }) {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const queryClient = useQueryClient()
  const [userId, setUserId] = useState('')
  const [role, setRole] = useState('OPERATOR')

  const membersQuery = useQuery({
    queryKey: ['projects', projectId, 'memberships'],
    queryFn: () => membershipsApi.list(projectId),
    enabled: !!projectId,
  })

  const usersQuery = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.list(),
    enabled: isAdmin,
  })

  const addMutation = useMutation({
    mutationFn: () => membershipsApi.add(projectId, { userId, role }),
    onSuccess: async () => {
      setUserId('')
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'memberships'] })
    },
  })

  const removeMutation = useMutation({
    mutationFn: (membershipId: string) => membershipsApi.remove(projectId, membershipId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'memberships'] })
    },
  })

  const rows = membersQuery.data ?? []
  const users = usersQuery.data ?? []

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
        Members
      </Typography>
      {isAdmin && (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }}>
          <TextField
            select
            label="User"
            size="small"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            sx={{ minWidth: 200 }}
          >
            {users.map((u) => (
              <MenuItem key={u.id} value={u.id}>
                {u.username} ({u.role})
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Role"
            size="small"
            value={role}
            onChange={(e) => setRole(e.target.value)}
            sx={{ minWidth: 140 }}
          >
            {['VIEWER', 'DEVELOPER', 'OPERATOR'].map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
          <Button
            variant="outlined"
            disabled={!userId || addMutation.isPending}
            onClick={() => addMutation.mutate()}
          >
            Add
          </Button>
        </Stack>
      )}
      {addMutation.isError && (
        <Alert severity="error" variant="outlined">
          Unable to add member (need OPERATOR membership or ADMIN).
        </Alert>
      )}
      <QueryState isLoading={membersQuery.isLoading} isError={membersQuery.isError}>
        {rows.length === 0 ? (
          <EmptyState title="No members" description="Project creator is added as OPERATOR automatically." />
        ) : (
          <DataTableFrame>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>User</TableCell>
                  <TableCell>Role</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((m) => {
                  const username = users.find((u) => u.id === m.userId)?.username
                  return (
                    <TableRow key={m.id}>
                      <TableCell className="atlas-mono">{username || m.userId.slice(0, 8)}</TableCell>
                      <TableCell>
                        <StatusChip label={m.role} />
                      </TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          color="error"
                          disabled={removeMutation.isPending}
                          onClick={() => removeMutation.mutate(m.id)}
                        >
                          Remove
                        </Button>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </DataTableFrame>
        )}
      </QueryState>
    </Stack>
  )
}
