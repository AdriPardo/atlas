import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Link,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined'
import { projectsApi } from '../../shared/api/endpoints'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import { StatusChip } from '../../shared/components/StatusChip'
import { useAuthReady } from '../auth/useAuthReady'

export function ProjectsListPage() {
  const [name, setName] = useState('')
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const authReady = useAuthReady()

  const query = useQuery({
    queryKey: ['projects', name],
    queryFn: () => projectsApi.list({ name: name || undefined, page: 0, size: 50 }),
    enabled: authReady,
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => projectsApi.remove(id),
    onSuccess: async () => {
      setDeleteId(null)
      setDeleteError(null)
      await queryClient.invalidateQueries({ queryKey: ['projects'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
    onError: () => setDeleteError('Unable to delete project. It may have deployments.'),
  })

  const rows = query.data?.content ?? []

  return (
    <PageShell>
      <PageHeader
        title="Projects"
        description="Projects group deployable services (repo + optional runtime path / atlas.yml)."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/projects/new')}>
            New project
          </Button>
        }
      />

      <DataTableFrame
        toolbar={
          <TextField
            label="Filter by name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            sx={{ maxWidth: 320, width: '100%' }}
          />
        }
      >
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<FolderOutlinedIcon />}
                title={name ? 'No matches' : 'No projects yet'}
                description={
                  name
                    ? 'Try a different name filter.'
                    : 'Create a project to register a repository and default service.'
                }
                actionLabel={name ? undefined : 'New project'}
                onAction={name ? undefined : () => navigate('/projects/new')}
              />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Slug</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((project) => (
                  <TableRow key={project.id} hover>
                    <TableCell>
                      <Link
                        component={RouterLink}
                        to={`/projects/${project.id}`}
                        underline="hover"
                        sx={{ fontWeight: 600 }}
                      >
                        {project.name}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono" color="text.secondary">
                        {project.slug}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <StatusChip label={project.status} />
                    </TableCell>
                    <TableCell align="right">
                      <RowOverflowMenu
                        aria-label={`Actions for ${project.name}`}
                        items={[
                          {
                            label: 'Edit',
                            onClick: () => navigate(`/projects/${project.id}/edit`),
                          },
                          {
                            label: 'Delete',
                            destructive: true,
                            dividerBefore: true,
                            onClick: () => setDeleteId(project.id),
                          },
                        ]}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete project"
        description="This action cannot be undone."
        error={deleteError}
        loading={removeMutation.isPending}
        onClose={() => {
          setDeleteId(null)
          setDeleteError(null)
        }}
        onConfirm={() => deleteId && removeMutation.mutate(deleteId)}
      />
    </PageShell>
  )
}
