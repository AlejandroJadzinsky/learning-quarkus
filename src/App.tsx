import { useState } from 'react';

type Approach = {
  id: string;
  title: string;
  icon: string;
  description: string;
  pros: string[];
  cons: string[];
  steps: { command: string; description: string }[];
  whenToUse: string;
};

const approaches: Approach[] = [
  {
    id: 'monorepo',
    title: 'Monorepo (Add a Folder)',
    icon: '📁',
    description:
      'Simply add a new folder alongside your existing "worktrees" project in the same repository. This is the simplest approach.',
    pros: [
      'Dead simple — just create a new directory',
      'Shared git history for all projects',
      'Easy to share common code between projects',
      'Single CI/CD pipeline can handle everything',
    ],
    cons: [
      'Repository grows larger over time',
      'All projects share the same branch/commit history',
      'Harder to separate permissions per project',
    ],
    steps: [
      {
        command: 'mkdir my-second-project',
        description: 'Create a new directory in your repo',
      },
      {
        command: 'cd my-second-project',
        description: 'Navigate into the new project folder',
      },
      {
        command: '# Add your project files here',
        description: 'Copy or create your project files',
      },
      {
        command: 'git add .\ngit commit -m "Add second project"',
        description: 'Stage and commit the new project',
      },
    ],
    whenToUse:
      'Best when projects are closely related, share dependencies, or are small enough that a single repo is manageable.',
  },
  {
    id: 'submodules',
    title: 'Git Submodules',
    icon: '🔗',
    description:
      'Keep each project in its own separate repository, then include them as submodules within a parent "umbrella" repository.',
    pros: [
      'Each project has its own independent repo & history',
      'Pin specific versions/commits of each project',
      'Teams can work on projects independently',
      'Clean separation of concerns',
    ],
    cons: [
      'More complex git workflow',
      'Requires extra commands to init/update submodules',
      'New contributors need to learn the submodule workflow',
      'CI/CD setup can be trickier',
    ],
    steps: [
      {
        command: '# First, create a separate repo for your second project',
        description: 'Push your second project to its own remote repo',
      },
      {
        command: 'git submodule add https://github.com/you/second-project.git',
        description: 'Add the second project as a submodule',
      },
      {
        command: 'git commit -m "Add second project as submodule"',
        description: 'Commit the submodule reference',
      },
      {
        command: '# When cloning the repo in the future:',
        description: 'Others need to clone with submodules',
      },
      {
        command: 'git clone --recurse-submodules https://github.com/you/repo.git',
        description: 'Clone with all submodules initialized',
      },
    ],
    whenToUse:
      'Best when projects are independent, have separate teams, or need to be versioned separately.',
  },
  {
    id: 'worktrees',
    title: 'Git Worktrees',
    icon: '🌳',
    description:
      'Use git worktrees to check out different branches (each representing a project) into separate directories, all sharing the same .git folder.',
    pros: [
      'No need to clone the repo multiple times',
      'Share the same .git directory (saves space)',
      'Work on multiple branches simultaneously',
      'Fast switching between projects/branches',
    ],
    cons: [
      'Each worktree is a branch — not truly separate projects',
      'Can get confusing with many worktrees',
      'All worktrees share the same remote',
      'Not ideal for fundamentally different projects',
    ],
    steps: [
      {
        command: 'git branch second-project',
        description: 'Create a new branch for the second project',
      },
      {
        command: 'git worktree add ../second-project second-project',
        description: 'Create a worktree in a sibling directory',
      },
      {
        command: 'cd ../second-project',
        description: 'Navigate to the new worktree',
      },
      {
        command: '# Work on your second project here',
        description: 'This directory is on the second-project branch',
      },
      {
        command: 'git worktree list',
        description: 'See all your worktrees',
      },
    ],
    whenToUse:
      'Best when "projects" are really different feature branches or versions of the same codebase.',
  },
  {
    id: 'packages',
    title: 'Package Manager Workspaces',
    icon: '📦',
    description:
      'Use a monorepo tool like npm/yarn/pnpm workspaces, Turborepo, or Nx to manage multiple projects in one repo with shared dependencies.',
    pros: [
      'Shared dependency management',
      'Built-in tooling for cross-project tasks',
      'Optimized builds (only rebuild what changed)',
      'Great DX with hot-reload across packages',
    ],
    cons: [
      'Requires setup and configuration',
      'Learning curve for workspace tools',
      'Overkill for just two small projects',
      'Lock files can get complex',
    ],
    steps: [
      {
        command: '# In your root package.json, add:',
        description: 'Configure workspaces',
      },
      {
        command: '{\n  "workspaces": [\n    "worktrees",\n    "my-second-project"\n  ]\n}',
        description: 'Define workspace packages',
      },
      {
        command: 'mkdir my-second-project\ncd my-second-project\nnpm init -y',
        description: 'Create and initialize the new package',
      },
      {
        command: 'npm install',
        description: 'Install all workspace dependencies',
      },
    ],
    whenToUse:
      'Best for JavaScript/TypeScript monorepos where projects share dependencies and you want optimized builds.',
  },
];

function CodeBlock({ code }: { code: string }) {
  return (
    <div className="relative group">
      <pre className="bg-gray-900 text-green-400 rounded-lg p-4 overflow-x-auto text-sm font-mono leading-relaxed">
        <code>{code}</code>
      </pre>
      <button
        onClick={() => navigator.clipboard.writeText(code)}
        className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity bg-gray-700 hover:bg-gray-600 text-gray-300 rounded px-2 py-1 text-xs"
        title="Copy to clipboard"
      >
        📋 Copy
      </button>
    </div>
  );
}

function ApproachCard({
  approach,
  isActive,
  onClick,
}: {
  approach: Approach;
  isActive: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`w-full text-left p-4 rounded-xl border-2 transition-all duration-200 ${
        isActive
          ? 'border-indigo-500 bg-indigo-50 shadow-md'
          : 'border-gray-200 bg-white hover:border-indigo-300 hover:shadow-sm'
      }`}
    >
      <div className="flex items-center gap-3">
        <span className="text-2xl">{approach.icon}</span>
        <div>
          <h3 className={`font-semibold ${isActive ? 'text-indigo-700' : 'text-gray-800'}`}>
            {approach.title}
          </h3>
          <p className="text-sm text-gray-500 mt-0.5 line-clamp-1">{approach.description}</p>
        </div>
      </div>
    </button>
  );
}

function ApproachDetail({ approach }: { approach: Approach }) {
  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Description */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <p className="text-gray-700 leading-relaxed">{approach.description}</p>
      </div>

      {/* Steps */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <h4 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
          <span className="text-indigo-500">▶</span> How to Do It
        </h4>
        <div className="space-y-4">
          {approach.steps.map((step, index) => (
            <div key={index} className="flex gap-4">
              <div className="flex-shrink-0 w-7 h-7 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-sm font-bold">
                {index + 1}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm text-gray-600 mb-2">{step.description}</p>
                <CodeBlock code={step.command} />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Pros & Cons */}
      <div className="grid md:grid-cols-2 gap-4">
        <div className="bg-green-50 rounded-xl border border-green-200 p-5">
          <h4 className="font-semibold text-green-800 mb-3 flex items-center gap-2">
            <span>✅</span> Pros
          </h4>
          <ul className="space-y-2">
            {approach.pros.map((pro, i) => (
              <li key={i} className="text-sm text-green-700 flex items-start gap-2">
                <span className="text-green-500 mt-0.5">•</span>
                {pro}
              </li>
            ))}
          </ul>
        </div>
        <div className="bg-red-50 rounded-xl border border-red-200 p-5">
          <h4 className="font-semibold text-red-800 mb-3 flex items-center gap-2">
            <span>⚠️</span> Cons
          </h4>
          <ul className="space-y-2">
            {approach.cons.map((con, i) => (
              <li key={i} className="text-sm text-red-700 flex items-start gap-2">
                <span className="text-red-500 mt-0.5">•</span>
                {con}
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* When to use */}
      <div className="bg-amber-50 rounded-xl border border-amber-200 p-5">
        <h4 className="font-semibold text-amber-800 mb-2 flex items-center gap-2">
          <span>💡</span> When to Use This Approach
        </h4>
        <p className="text-sm text-amber-700">{approach.whenToUse}</p>
      </div>
    </div>
  );
}

function RepoStructure() {
  return (
    <div className="bg-gray-900 rounded-xl p-6 text-sm font-mono">
      <div className="text-gray-400 mb-3">Your repository structure:</div>
      <div className="space-y-1">
        <div className="text-yellow-300">📂 my-repository/</div>
        <div className="ml-4 text-gray-300">
          <span className="text-blue-400">├──</span> .git/
        </div>
        <div className="ml-4 text-gray-300">
          <span className="text-blue-400">├──</span> README.md
        </div>
        <div className="ml-4 text-green-400 font-semibold">
          <span className="text-blue-400">├──</span> 📂 worktrees/{' '}
          <span className="text-gray-500 text-xs">(existing)</span>
        </div>
        <div className="ml-4 text-purple-400 font-semibold">
          <span className="text-blue-400">└──</span> 📂 my-second-project/{' '}
          <span className="text-gray-500 text-xs">(new! ✨)</span>
        </div>
      </div>
    </div>
  );
}

export default function App() {
  const [activeApproach, setActiveApproach] = useState('monorepo');
  const activeData = approaches.find((a) => a.id === activeApproach)!;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-indigo-50">
      {/* Header */}
      <header className="bg-white/80 backdrop-blur-sm border-b border-gray-200 sticky top-0 z-10">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center gap-3">
          <div className="w-10 h-10 bg-indigo-600 rounded-lg flex items-center justify-center text-white text-lg">
            📦
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">Adding a Second Project</h1>
            <p className="text-sm text-gray-500">to your Git repository</p>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-8">
        {/* Intro Section */}
        <div className="mb-8">
          <div className="bg-white rounded-2xl border border-gray-200 p-6 md:p-8 shadow-sm">
            <h2 className="text-2xl font-bold text-gray-900 mb-3">
              You have a repo with a project called <span className="text-indigo-600">"worktrees"</span> — now what?
            </h2>
            <p className="text-gray-600 leading-relaxed mb-4">
              There are several ways to add a second project to your repository. The best approach depends on how
              related the projects are, whether they need independent versioning, and your team's workflow preferences.
            </p>
            <RepoStructure />
          </div>
        </div>

        {/* Quick Answer */}
        <div className="mb-8 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-2xl p-6 md:p-8 text-white shadow-lg">
          <h3 className="text-lg font-semibold mb-2 flex items-center gap-2">
            <span>⚡</span> Quick Answer
          </h3>
          <p className="text-indigo-100 leading-relaxed">
            The simplest way is to just <strong className="text-white">create a new folder</strong> in your repository
            and add your project files there. Git doesn't care about project boundaries — it just tracks files.
            Run <code className="bg-white/20 px-2 py-0.5 rounded text-sm">mkdir my-second-project</code> and start
            adding files!
          </p>
        </div>

        {/* Approaches */}
        <div className="mb-6">
          <h2 className="text-xl font-bold text-gray-900 mb-1">Choose Your Approach</h2>
          <p className="text-gray-500 text-sm">Click each option to see detailed instructions</p>
        </div>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Sidebar - Approach selector */}
          <div className="lg:col-span-1 space-y-3">
            {approaches.map((approach) => (
              <ApproachCard
                key={approach.id}
                approach={approach}
                isActive={activeApproach === approach.id}
                onClick={() => setActiveApproach(approach.id)}
              />
            ))}
          </div>

          {/* Main content - Approach details */}
          <div className="lg:col-span-2">
            <ApproachDetail approach={activeData} />
          </div>
        </div>

        {/* TL;DR Table */}
        <div className="mt-12 bg-white rounded-2xl border border-gray-200 p-6 md:p-8 shadow-sm">
          <h2 className="text-xl font-bold text-gray-900 mb-4">📊 Comparison at a Glance</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-2 font-semibold text-gray-700">Approach</th>
                  <th className="text-left py-3 px-2 font-semibold text-gray-700">Complexity</th>
                  <th className="text-left py-3 px-2 font-semibold text-gray-700">Best For</th>
                  <th className="text-left py-3 px-2 font-semibold text-gray-700">Independence</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-gray-100">
                  <td className="py-3 px-2 font-medium">📁 Monorepo</td>
                  <td className="py-3 px-2">
                    <span className="bg-green-100 text-green-700 px-2 py-0.5 rounded-full text-xs font-medium">
                      Easy
                    </span>
                  </td>
                  <td className="py-3 px-2 text-gray-600">Related projects</td>
                  <td className="py-3 px-2 text-gray-600">Low</td>
                </tr>
                <tr className="border-b border-gray-100">
                  <td className="py-3 px-2 font-medium">🔗 Submodules</td>
                  <td className="py-3 px-2">
                    <span className="bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded-full text-xs font-medium">
                      Medium
                    </span>
                  </td>
                  <td className="py-3 px-2 text-gray-600">Independent projects</td>
                  <td className="py-3 px-2 text-gray-600">High</td>
                </tr>
                <tr className="border-b border-gray-100">
                  <td className="py-3 px-2 font-medium">🌳 Worktrees</td>
                  <td className="py-3 px-2">
                    <span className="bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded-full text-xs font-medium">
                      Medium
                    </span>
                  </td>
                  <td className="py-3 px-2 text-gray-600">Branch-based dev</td>
                  <td className="py-3 px-2 text-gray-600">Medium</td>
                </tr>
                <tr>
                  <td className="py-3 px-2 font-medium">📦 Workspaces</td>
                  <td className="py-3 px-2">
                    <span className="bg-red-100 text-red-700 px-2 py-0.5 rounded-full text-xs font-medium">
                      Advanced
                    </span>
                  </td>
                  <td className="py-3 px-2 text-gray-600">JS/TS monorepos</td>
                  <td className="py-3 px-2 text-gray-600">Medium</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Footer tip */}
        <div className="mt-8 text-center text-sm text-gray-400 pb-8">
          <p>💡 Tip: When in doubt, start simple with a folder. You can always restructure later!</p>
        </div>
      </main>
    </div>
  );
}
