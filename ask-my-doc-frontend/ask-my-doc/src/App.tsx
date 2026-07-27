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
import {doLogout, getUsername} from './keycloak';
import DocumentSearchNonAi from "./components/DocumentSearchNonAi.tsx";
import {fetchAppConfig} from "./components/FetchAppConfig.tsx";
import LayoutContainer from "./components/ui/LayoutContainer.tsx";

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
                        />
                    )}
                </LayoutContainer>
            </main>

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