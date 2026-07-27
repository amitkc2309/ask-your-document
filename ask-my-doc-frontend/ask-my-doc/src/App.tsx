import {useEffect, useState} from 'react';
import DocumentUpload from './components/DocumentUpload';
import DocumentList from './components/DocumentList';
import Chat from './components/Chat.tsx';
import {
    DocumentPlusIcon,
    DocumentTextIcon,
    MagnifyingGlassIcon,
    Squares2X2Icon,
    ArrowRightOnRectangleIcon,
    UserCircleIcon, SparklesIcon
} from '@heroicons/react/24/outline';
import {doLogout, getToken, getUsername} from './keycloak';
import DocumentSearchNonAi from "./components/DocumentSearchNonAi.tsx";
import {fetchAppConfig} from "./components/FetchAppConfig.tsx";
import LayoutContainer from "./components/ui/LayoutContainer.tsx";
import appconfig from "./config/config.ts";
import {fetchEventSource} from '@microsoft/fetch-event-source';

const steps = [
    "UPLOADING",
    "UPLOADED",
    "QUEUED",
    "PROCESSING",
    "EMBEDDING",
    "INDEXED",
    "READY"
];

function StatusRow({current, step, label}: {
    current: string;
    step: string;
    label: string;
}) {

    const currentIndex = steps.indexOf(current);
    const stepIndex = steps.indexOf(step);

    let icon = "○";
    let color = "text-gray-400";

    if (stepIndex < currentIndex) {
        icon = "✓";
        color = "text-green-600";
    } else if (stepIndex === currentIndex) {
        icon = "●";
        color = "text-blue-600 font-semibold";
    }

    return (
        <div className={`flex items-center py-1 ${color}`}>
            <span className="w-6">{icon}</span>
            <span>{label}</span>
        </div>
    );
}

function App() {
    const [config, setConfig] = useState(null);
    const [activeTab, setActiveTab] = useState('search');
    const username = getUsername();

    // Lifted State for DocumentList
    const [documents, setDocuments] = useState<any[]>([]);
    const [listSearchType, setListSearchType] = useState('all');
    const [listSearchQuery, setListSearchQuery] = useState('');

    // Lifted State for DocumentUpload
    const [uploadTitle, setUploadTitle] = useState('');
    const [uploadAuthor, setUploadAuthor] = useState('');
    const [uploadFile, setUploadFile] = useState<File | null>(null);
    const [abortController, setAbortController] = useState<AbortController | null>(null);
    const [uploadStatus, setUploadStatus] = useState("UPLOADING");
    const [showUploadPopup, setShowUploadPopup] = useState(false);

    // Lifted State for DocumentSearch
    const [searchQuestion, setSearchQuestion] = useState('');
    const [searchResults, setSearchResults] = useState<any>(null);
    useEffect(() => {
        fetchAppConfig()
            .then(setConfig)
            .catch(console.error);
    }, []);
    if (!config) {
        return <div>Loading...</div>;
    }

    const tabs = [
        {
            id: 'search',
            name: config.aiMode ? 'Ask AI' : 'Search',
            icon: config.aiMode ? SparklesIcon : MagnifyingGlassIcon,
        },
        {
            id: 'upload',
            name: 'Upload',
            icon: DocumentPlusIcon,
        },
        {
            id: 'list',
            name: 'Manage',
            icon: Squares2X2Icon,
        },
    ];

    const handleDocumentUploaded = async (documentId: string) => {
        abortController?.abort();
        const controller = new AbortController();
        setAbortController(controller);
        setShowUploadPopup(true);
        setUploadStatus("UPLOADED");
        await fetchEventSource(
            `${appconfig.apiUrl}/manage/documents/${documentId}/events`,
            {
                signal: controller.signal,
                headers: {
                    Authorization: `Bearer ${getToken()}`
                },
                onmessage(event) {
                    const status = JSON.parse(event.data);
                    setUploadStatus(status.documentStatus);
                    if (status.documentStatus === "READY") {
                        setTimeout(() => {
                            setShowUploadPopup(false);
                        }, 3000);
                    }
                    if (status.documentStatus === "FAILED") {
                        setTimeout(() => setShowUploadPopup(false), 3000);
                        controller.abort();
                    }
                },
                onerror(err) {
                    console.error(err);
                }
            }
        );
    };

    return (
        <div className="min-h-screen bg-background">
            {/* Header */}
            <header className="bg-background border-b border-border">
                <LayoutContainer>
                    <div className="flex justify-between h-16 items-center">
                        <div className="flex items-center">
                            <DocumentTextIcon className="h-8 w-8 text-primary"/>
                            <h1 className="ml-2 text-xl font-bold text-foreground">Ask My Doc</h1>
                        </div>
                        <div className="flex items-center space-x-4">
                            <div className="flex items-center text-muted-foreground">
                                <UserCircleIcon className="h-6 w-6 mr-1"/>
                                <span className="text-sm font-medium">{username}</span>
                            </div>
                            <button
                                onClick={() => doLogout()}
                                className="inline-flex items-center px-3 py-1.5 border border-transparent text-sm font-medium rounded-md text-primary-foreground bg-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring transition-colors"
                            >
                                <ArrowRightOnRectangleIcon className="h-4 w-4 mr-1"/>
                                Logout
                            </button>
                        </div>
                    </div>
                </LayoutContainer>
            </header>

            {/* Navigation */}
            <nav>
                <LayoutContainer>
                    <div className="border-b border-border">
                        <div className="flex space-x-8" aria-label="Tabs">
                            {tabs.map((tab) => (
                                <button
                                    key={tab.id}
                                    onClick={() => setActiveTab(tab.id)}
                                    className={`
                  ${
                                        activeTab === tab.id
                                            ? 'border-primary text-primary'
                                            : 'border-transparent text-muted-foreground hover:text-foreground hover:border-border'
                                    }
                  group inline-flex items-center py-4 px-1 border-b-2 font-medium text-sm
                `}
                                >
                                    <tab.icon
                                        className={`
                    ${
                                            activeTab === tab.id
                                                ? 'text-primary'
                                                : 'text-muted-foreground group-hover:text-foreground'
                                        }
                    -ml-0.5 mr-2 h-5 w-5
                  `}
                                        aria-hidden="true"
                                    />
                                    <span>{tab.name}</span>
                                </button>
                            ))}
                        </div>
                    </div>
                </LayoutContainer>
            </nav>

            {/* Main Content */}
            <main className="py-6">
                <LayoutContainer>
                    {activeTab === 'search' && config.aiMode && (
                        <Chat
                            question={searchQuestion}
                            setQuestion={setSearchQuestion}
                            searchResults={searchResults}
                            setSearchResults={setSearchResults}
                        />
                    )}
                    {activeTab === 'search' && !(config.aiMode) && (
                        <DocumentSearchNonAi
                            question={searchQuestion}
                            setQuestion={setSearchQuestion}
                            searchResults={searchResults}
                            setSearchResults={setSearchResults}
                        />
                    )}
                    {activeTab === 'list' && (
                        <DocumentList
                            documents={documents}
                            setDocuments={setDocuments}
                            searchType={listSearchType}
                            setSearchType={setListSearchType}
                            searchQuery={listSearchQuery}
                            setSearchQuery={setListSearchQuery}
                        />
                    )}
                    {activeTab === 'upload' && (
                        <DocumentUpload
                            title={uploadTitle}
                            setTitle={setUploadTitle}
                            author={uploadAuthor}
                            setAuthor={setUploadAuthor}
                            file={uploadFile}
                            setFile={setUploadFile}
                            onDocumentUploaded={handleDocumentUploaded}
                        />
                    )}
                </LayoutContainer>
            </main>
            {showUploadPopup && (
                <div
                    className="fixed bottom-6 right-6 w-96 rounded-xl bg-white shadow-2xl border border-gray-200 p-5 z-50">
                    <div className="flex items-center justify-between mb-4">
                        <h3 className="font-semibold text-lg">
                            📄 Processing Document
                        </h3>
                        {uploadStatus !== "READY" &&
                            uploadStatus !== "FAILED" && (
                                <div
                                    className="h-5 w-5 rounded-full border-2 border-blue-600 border-t-transparent animate-spin"/>
                            )}

                    </div>
                    <StatusRow
                        current={uploadStatus}
                        step="UPLOADED"
                        label="Upload Completed"
                    />
                    <StatusRow
                        current={uploadStatus}
                        step="QUEUED"
                        label="Waiting for Server"
                    />
                    <StatusRow
                        current={uploadStatus}
                        step="PROCESSING"
                        label="Extracting Text"
                    />
                    <StatusRow
                        current={uploadStatus}
                        step="EMBEDDING"
                        label="Generating Embeddings"
                    />
                    <StatusRow
                        current={uploadStatus}
                        step="INDEXED"
                        label="Indexing Document"
                    />
                    <StatusRow
                        current={uploadStatus}
                        step="READY"
                        label="Ready"
                    />
                    {uploadStatus === "READY" && (
                        <div className="mt-4 rounded-lg bg-green-50 p-3 text-green-700 font-medium">
                            ✅ Document is now searchable.
                        </div>
                    )}
                    {uploadStatus === "FAILED" && (
                        <div className="mt-4 rounded-lg bg-red-50 p-3 text-red-700 font-medium">
                            ❌ Processing failed.
                        </div>
                    )}
                </div>
            )}

            {/* Footer */}
            <footer className="bg-background border-t border-border mt-auto">
                <LayoutContainer>
                    <p className="text-center text-sm text-muted-foreground">
                        Ask My Doc - Document Management and Search System
                    </p>
                </LayoutContainer>
            </footer>
        </div>
    );
}

export default App;