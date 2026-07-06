import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

/**
 * FP-018 Task 05: Frontend Actor Boundary Cleanup + Auth Boundary Regression
 *
 * Proves the frontend admin code does not introduce production auth,
 * does not expose forbidden credential/token fields, does not add
 * publish/export/delivery controls, and keeps actorRole as workflow context only.
 */

const SRC_DIR = path.resolve(__dirname, '../../../')

function readAllTsFiles(dir: string): string[] {
  const results: string[] = []
  if (!fs.existsSync(dir)) return results
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory() && entry.name !== 'node_modules' && entry.name !== '__tests__') {
      results.push(...readAllTsFiles(fullPath))
    } else if (entry.isFile() && (entry.name.endsWith('.ts') || entry.name.endsWith('.tsx'))) {
      results.push(fullPath)
    }
  }
  return results
}

function readAllTestFiles(dir: string): string[] {
  const results: string[] = []
  if (!fs.existsSync(dir)) return results
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      results.push(...readAllTestFiles(fullPath))
    } else if (entry.isFile() && entry.name.endsWith('.test.ts')) {
      results.push(fullPath)
    }
  }
  return results
}

function getAllSourceContent(): string {
  const files = readAllTsFiles(SRC_DIR)
  return files.map(f => fs.readFileSync(f, 'utf-8')).join('\n')
}

const FORBIDDEN_AUTH_FIELDS = [
  'token', 'jwt', 'session', 'credential', 'password',
  'secret', 'apiKey', 'accessKey', 'principalRaw',
  'rawPrincipal', 'authorizationHeader', 'bearerToken',
  'refreshToken', 'idToken', 'accessToken',
]

const FORBIDDEN_DELIVERY_FIELDS = [
  'publicUrl', 'signedUrl', 'cdnUrl', 'providerUrl',
  'downloadUrl', 'exportUrl', 'publishUrl',
]

const FORBIDDEN_STORAGE_FIELDS = [
  'storageKey', 'bucket', 'objectPath', 'rawPayload', 'base64Content',
]

describe('FP-018 Task 05 — Frontend Actor Boundary Cleanup', () => {

  describe('A. No Production Auth Patterns', () => {
    it('no auth/credential/token type fields in source', () => {
      const allContent = getAllSourceContent()
      for (const field of FORBIDDEN_AUTH_FIELDS) {
        // Check for TypeScript field declarations: fieldName: or fieldName?:
        const fieldPattern = new RegExp(`\\b${field}\\s*[?]?\\s*:`, 'i')
        expect(allContent).not.toMatch(fieldPattern)
      }
    })

    it('no login/signIn/signOut/authenticate patterns', () => {
      const allContent = getAllSourceContent()
      for (const pattern of ['login', 'signIn', 'signOut', 'authenticate', 'logout']) {
        const regex = new RegExp(`\\b${pattern}\\b`, 'i')
        expect(allContent).not.toMatch(regex)
      }
    })

    it('no Authorization header usage', () => {
      const allContent = getAllSourceContent()
      expect(allContent).not.toContain("'Authorization'")
      expect(allContent).not.toContain('"Authorization"')
    })

    it('no Spring Security or JWT library imports', () => {
      const allContent = getAllSourceContent()
      expect(allContent).not.toContain('jsonwebtoken')
      expect(allContent).not.toContain('jose')
      expect(allContent).not.toContain('passport')
      expect(allContent).not.toContain('next-auth')
    })
  })

  describe('B. No Publish/Export/Delivery Controls', () => {
    it('no delivery URL fields in types', () => {
      const allContent = getAllSourceContent()
      for (const field of FORBIDDEN_DELIVERY_FIELDS) {
        const fieldPattern = new RegExp(`\\b${field}\\s*[?]?\\s*:`, 'i')
        expect(allContent).not.toMatch(fieldPattern)
      }
    })

    it('no storage/raw/base64 fields in review types', () => {
      const typesFile = fs.readFileSync(
        path.resolve(SRC_DIR, 'types/index.ts'), 'utf-8'
      )
      // Extract only review-related type blocks (after ReviewActorRole definition)
      const reviewSection = typesFile.substring(
        typesFile.indexOf("ReviewActorRole")
      )
      for (const field of FORBIDDEN_STORAGE_FIELDS) {
        const fieldPattern = new RegExp(`\\b${field}\\s*[?]?\\s*:`, 'i')
        expect(reviewSection).not.toMatch(fieldPattern)
      }
    })

    it('no publish/export button or action in source', () => {
      const allContent = getAllSourceContent()
      expect(allContent).not.toContain("'PUBLISH'")
      expect(allContent).not.toContain('"PUBLISH"')
      expect(allContent).not.toContain("'EXPORT'")
      expect(allContent).not.toContain('"EXPORT"')
    })
  })

  describe('C. actorRole is Workflow Context Only', () => {
    it('ReviewActorRole enum has only CREATOR, REVIEWER, ADMIN, SYSTEM', () => {
      const typesFile = fs.readFileSync(
        path.resolve(SRC_DIR, 'types/index.ts'), 'utf-8'
      )
      expect(typesFile).toContain("'CREATOR'")
      expect(typesFile).toContain("'REVIEWER'")
      expect(typesFile).toContain("'ADMIN'")
      expect(typesFile).toContain("'SYSTEM'")
      // No production auth roles
      expect(typesFile).not.toContain("'AUTHENTICATED'")
      expect(typesFile).not.toContain("'PRINCIPAL'")
      expect(typesFile).not.toContain("'SUPER_ADMIN'")
    })

    it('actorRole property exists in request types for compatibility', () => {
      const typesFile = fs.readFileSync(
        path.resolve(SRC_DIR, 'types/index.ts'), 'utf-8'
      )
      expect(typesFile).toContain('actorRole: ReviewActorRole')
    })

    it('no "authenticated" or "production auth" label in UI source', () => {
      const allContent = getAllSourceContent()
      expect(allContent.toLowerCase()).not.toContain('production auth')
      expect(allContent.toLowerCase()).not.toContain('authenticated user')
      expect(allContent.toLowerCase()).not.toContain('logged in')
    })
  })

  describe('D. API Client Safety', () => {
    it('reviewApi does not send Authorization header', () => {
      const apiFile = fs.readFileSync(
        path.resolve(SRC_DIR, 'api/reviewApi.ts'), 'utf-8'
      )
      expect(apiFile).not.toContain('Authorization')
      expect(apiFile).not.toContain('Bearer')
    })

    it('API base paths use /api/admin/ prefix', () => {
      const apiFile = fs.readFileSync(
        path.resolve(SRC_DIR, 'api/reviewApi.ts'), 'utf-8'
      )
      expect(apiFile).toContain('/admin/')
    })

    it('no publish/export API methods', () => {
      const apiFile = fs.readFileSync(
        path.resolve(SRC_DIR, 'api/reviewApi.ts'), 'utf-8'
      )
      // Check for publish/export API method names, not the JS 'export' keyword
      expect(apiFile).not.toContain('publishContent')
      expect(apiFile).not.toContain('exportContent')
      expect(apiFile).not.toContain('downloadContent')
    })
  })

  describe('E. No Unsafe HTML Patterns', () => {
    it('no dangerouslySetInnerHTML usage in review component code', () => {
      const reviewDir = path.resolve(SRC_DIR, 'components/review')
      const files = fs.readdirSync(reviewDir)
        .filter(f => f.endsWith('.tsx'))
        .map(f => path.join(reviewDir, f))
      for (const file of files) {
        const content = fs.readFileSync(file, 'utf-8')
        // Remove comments before checking
        const codeOnly = content
          .replace(/\/\*[\s\S]*?\*\//g, '')
          .replace(/\/\/.*/g, '')
        expect(codeOnly).not.toContain('dangerouslySetInnerHTML')
      }
    })

    it('no innerHTML assignment in review component code', () => {
      const reviewDir = path.resolve(SRC_DIR, 'components/review')
      const files = fs.readdirSync(reviewDir)
        .filter(f => f.endsWith('.tsx'))
        .map(f => path.join(reviewDir, f))
      for (const file of files) {
        const content = fs.readFileSync(file, 'utf-8')
        const codeOnly = content
          .replace(/\/\*[\s\S]*?\*\//g, '')
          .replace(/\/\/.*/g, '')
        expect(codeOnly).not.toContain('.innerHTML')
      }
    })
  })

  describe('F. Existing Test Inventory', () => {
    it('review test directory has expected test files', () => {
      const testDir = path.resolve(SRC_DIR, 'components/review/__tests__')
      const files = fs.readdirSync(testDir).filter(f => f.endsWith('.test.ts'))
      expect(files.length).toBeGreaterThanOrEqual(10)
    })

    it('ReviewConsole test checks CREATOR/SYSTEM restrictions', () => {
      const testFile = fs.readFileSync(
        path.resolve(SRC_DIR, 'components/review/__tests__/ReviewConsole.test.ts'), 'utf-8'
      )
      expect(testFile).toContain('CREATOR')
      expect(testFile).toContain('SYSTEM')
    })
  })
})
