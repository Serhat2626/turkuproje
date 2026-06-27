import { Search, FileText, Film, Image, Mic, Video, Send, Check } from 'lucide-react'

const STEPS = [
  { key: 'research', label: 'Araştırma', icon: Search, minStatus: 'RESEARCHED' },
  { key: 'script', label: 'Senaryo', icon: FileText, minStatus: 'SCRIPT_GENERATED' },
  { key: 'storyboard', label: 'Storyboard', icon: Film, minStatus: 'STORYBOARD_GENERATED' },
  { key: 'visuals', label: 'Görseller', icon: Image, minStatus: 'MEDIA_PROMPTS_GENERATED' },
  { key: 'audio', label: 'Ses & Müzik', icon: Mic, minStatus: 'VOICE_READY' },
  { key: 'video', label: 'Video', icon: Video, minStatus: 'VIDEO_READY' },
  { key: 'publish', label: 'Yayın', icon: Send, minStatus: 'PUBLISHED' },
]

const STATUS_ORDER = ['DRAFT','RESEARCHED','SCRIPT_GENERATED','STORYBOARD_GENERATED','MEDIA_PROMPTS_GENERATED','VOICE_READY','VIDEO_READY','PUBLISHED']

export default function PipelineStepper({ status, activeTab, onTabChange }) {
  const statusIndex = STATUS_ORDER.indexOf(status)
  return (
    <div className="pipeline-stepper">
      {STEPS.map((step, index) => {
        const stepStatusIndex = STATUS_ORDER.indexOf(step.minStatus)
        const isCompleted = statusIndex >= stepStatusIndex
        const isActive = activeTab === step.key
        const Icon = step.icon
        return (
          <div key={step.key} style={{ display: 'flex', alignItems: 'center' }}>
            <div className={`step ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`} onClick={() => onTabChange(step.key)}>
              <div className="step-icon">{isCompleted ? <Check size={14} /> : <Icon size={14} />}</div>
              {step.label}
            </div>
            {index < STEPS.length - 1 && <div className={`step-connector ${isCompleted ? 'completed' : ''}`} />}
          </div>
        )
      })}
    </div>
  )
}
