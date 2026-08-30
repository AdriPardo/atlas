import { createRoot } from 'react-dom/client'
import { App } from './app/App'
import { bootstrapAuthSession } from './shared/api/authSession'
import './index.css'

const rootEl = document.getElementById('root')!

void bootstrapAuthSession().then(() => {
  createRoot(rootEl).render(<App />)
})
