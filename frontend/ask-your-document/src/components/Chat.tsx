import {useEffect, useState, useRef} from 'react';
import {ChatBubbleLeftIcon, TrashIcon} from '@heroicons/react/24/outline';
import config from '../config/config';
import {getToken, updateToken} from "../keycloak.ts";
import LayoutContainer from "./ui/LayoutContainer.tsx";
import {PanelLeftClose, PanelLeftOpen, PlusIcon, SendHorizonalIcon, Square} from "lucide-react";
import {fetchAvailableModels} from "./FetchAvailableModels.tsx";

import {
    createNewChat,
    getAllConversations,
    getConversation,
    deleteConversation
} from './ChatApi.tsx';

interface Message {
    messageType: string;
    text: string;
    [key: string]: any;
}

interface Conversation {
    id?: string;
    conversationId?: string;
    title?: string;
    [key: string]: any;
}

interface ChatProps {
    question: string;
    setQuestion: (question: string) => void;
    searchResults?: any;
    setSearchResults: (results: any) => void;
}

const Chat = ({
                  question,
                  setQuestion,
                  setSearchResults
              }: ChatProps) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [availableModels, setAvailableModels] = useState<Record<string, string[]>>({});
    const [aiProvider, setAiProvider] = useState('');
    const [modelName, setModelName] = useState('');

    const [conversations, setConversations] = useState<Conversation[]>([]);
    const [conversationId, setConversationId] = useState<string | null>(null);

    const [messages, setMessages] = useState<Message[]>([]);

    const [sidebarOpen, setSidebarOpen] = useState(true);

    const abortControllerRef = useRef<AbortController | null>(null);
    const messagesEndRef = useRef<HTMLDivElement | null>(null);

    /*
     * Load AI providers/models
     */
    useEffect(() => {
        fetchAvailableModels()
            .then((models) => {
                setAvailableModels(models);

                const providers = Object.keys(models);

                if (providers.length > 0) {
                    const defaultProvider = providers[0];

                    setAiProvider(defaultProvider);

                    setModelName(
                        models[defaultProvider]?.[0] || ''
                    );
                }
            })
            .catch((err) => {
                console.error(err);
                setError('Failed to load AI models');
            });
    }, []);

    /*
     * Load user's conversations
     */
    useEffect(() => {
        loadConversations();
    }, []);

    /*
     * Scroll to bottom whenever messages change
     */
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({
            behavior: 'smooth'
        });
    }, [messages]);

    const loadConversations = async () => {
        try {
            const data = await getAllConversations();

            setConversations(data || []);
        } catch (err) {
            console.error(err);
            setError('Failed to load conversations');
        }
    };

    /*
     * Start a completely new chat
     */
    const handleNewChat = async () => {
        try {
            // Stop any currently running stream
            abortControllerRef.current?.abort();

            setLoading(false);
            setError('');

            const newConversationId = await createNewChat();

            setConversationId(newConversationId);

            setMessages([]);

            setQuestion('');

            setSearchResults({
                answer: '',
                snippets: []
            });

            /*
             * Refresh sidebar because the new conversation
             * now exists in the backend.
             */
            await loadConversations();

        } catch (err) {
            console.error(err);
            setError('Failed to create a new chat');
        }
    };

    /*
     * Open an existing conversation
     */
    const handleSelectConversation = async (id: string) => {
        if (loading) {
            return;
        }

        try {
            setError('');

            const conversation = await getConversation(id);

            setConversationId(id);

            /*
             * Backend:
             *
             * [
             *   {
             *     messageType: "USER",
             *     text: "Ram Age?"
             *   },
             *   {
             *     messageType: "ASSISTANT",
             *     text: "21 years old."
             *   }
             * ]
             */
            setMessages(conversation || []);

            setQuestion('');

            setSearchResults({
                answer: '',
                snippets: []
            });

        } catch (err) {
            console.error(err);
            setError('Failed to load conversation');
        }
    };

    /*
     * Delete conversation
     */
    const handleDeleteConversation = async (id: string) => {
        try {
            await deleteConversation(id);

            /*
             * If deleting currently selected chat,
             * clear the main window.
             */
            if (id === conversationId) {
                setConversationId(null);
                setMessages([]);
                setQuestion('');
            }

            await loadConversations();

        } catch (err) {
            console.error(err);
            setError('Failed to delete conversation');
        }
    };

    /*
     * Send message and stream AI response
     */
    const handleSearch = async (e?: React.SyntheticEvent) => {
        e?.preventDefault();

        const text = question.trim();

        if (!text || loading) {
            return;
        }

        /*
         * If there isn't a conversation yet,
         * create one first.
         */
        let currentConversationId = conversationId;

        try {
            setError('');

            if (!currentConversationId) {
                currentConversationId = await createNewChat();

                setConversationId(currentConversationId);

                await loadConversations();
            }

            /*
             * Immediately add USER message to UI.
             */
            const userMessage: Message = {
                messageType: 'USER',
                text
            };

            setMessages((prev) => [
                ...prev,
                userMessage
            ]);

            setQuestion('');

            setLoading(true);

            /*
             * Add empty AI message.
             *
             * We will update this as tokens arrive.
             */
            setMessages((prev) => [
                ...prev,
                {
                    messageType: 'ASSISTANT',
                    text: ''
                }
            ]);

            /*
             * Refresh token
             */
            await new Promise<void>((resolve) => {
                updateToken(() => resolve());
            });

            const token = getToken();

            const controller = new AbortController();

            abortControllerRef.current = controller;

            const response = await fetch(
                `${config.apiUrl}/ai/chat`,
                {
                    method: 'POST',

                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'text/event-stream',
                        'Authorization': `Bearer ${token}`
                    },

                    body: JSON.stringify({
                        userMessage: text,
                        maxResults: 5,
                        snippetLength: 200,
                        conversationId: currentConversationId,
                        aiRequest: {
                            aiProvider,
                            modelName
                        }
                    }),

                    signal: controller.signal
                }
            );

            if (!response.ok) {
                throw new Error(
                    `HTTP ${response.status}`
                );
            }

            if (!response.body) {
                throw new Error(
                    'No response body'
                );
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';
            let aiText = '';
            while (true) {
                const {
                    done,
                    value
                } = await reader.read();

                if (done) {
                    break;
                }

                buffer += decoder.decode(
                    value,
                    {
                        stream: true
                    }
                );

                /*
                 * SSE events are separated by
                 * an empty line.
                 */
                const events = buffer.split(/\r?\n\r?\n/);
                buffer = events.pop() || '';
                for (const event of events) {
                    const lines = event.split(/\r?\n/);
                    const dataLines = [];
                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            dataLines.push(
                                line.substring(5)
                            );
                        }
                    }
                    const data = dataLines.join('\n');
                    if (!data) {
                        continue;
                    }
                    aiText += data;
                    /*
                     * Update the LAST message,
                     * which is our assistant message.
                     */
                    setMessages((prev) => {

                        if (prev.length === 0) {
                            return prev;
                        }

                        const updated = [
                            ...prev
                        ];

                        const lastIndex =
                            updated.length - 1;

                        updated[lastIndex] = {
                            ...updated[lastIndex],
                            text: aiText
                        };
                        return updated;
                    });
                }
            }

            /*
             * Refresh conversation list because
             * its title / updated time may have changed.
             */
            await loadConversations();

        } catch (err: any) {

            if (err?.name === 'AbortError') {

                setMessages((prev) => {

                    if (prev.length === 0) {
                        return prev;
                    }

                    const updated = [
                        ...prev
                    ];

                    const lastIndex =
                        updated.length - 1;

                    updated[lastIndex] = {
                        ...updated[lastIndex],
                        text:
                            `${updated[lastIndex].text || ''}\n\n[Cancelled]`
                    };

                    return updated;
                });

            } else {

                console.error(
                    'Streaming failed:',
                    err
                );

                setError(
                    'Streaming failed'
                );

            }

        } finally {

            setLoading(false);

            abortControllerRef.current =
                null;
        }
    };

    const handleCancel = () => {
        abortControllerRef.current?.abort();
    };

    return (
        <LayoutContainer>

            <div className="flex h-[calc(100vh-120px)] min-h-[600px] bg-background border border-border rounded-xl overflow-hidden shadow-sm">

                {/* =====================================================
                    LEFT SIDEBAR
                ====================================================== */}

                {sidebarOpen && (
                    <aside className="w-72 shrink-0 border-r border-border bg-muted/30 flex flex-col">

                        {/* Sidebar header */}
                        <div className="p-4 border-b border-border">

                            <button
                                onClick={handleNewChat}
                                disabled={loading}
                                className="w-full flex items-center justify-center gap-2 px-4 py-3 rounded-lg bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                            >
                                <PlusIcon className="h-5 w-5" />

                                New Chat
                            </button>

                        </div>

                        {/* Conversations */}
                        <div className="flex-1 overflow-y-auto p-3">

                            <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wider px-2 mb-2">
                                Your Chats
                            </div>

                            {conversations.length === 0 && (
                                <div className="px-2 py-6 text-sm text-muted-foreground text-center">
                                    No conversations yet
                                </div>
                            )}

                            <div className="space-y-1">

                                {conversations.map((conversation) => {

                                    /*
                                     * Adjust these property names if
                                     * your ChatSessionsDto uses different
                                     * names.
                                     */
                                    const id =
                                        conversation.id ||
                                        conversation.conversationId;

                                    const title =
                                        conversation.title;

                                    return (
                                        <div
                                            key={id}
                                            className={`group flex items-center gap-2 rounded-lg ${
                                                id === conversationId
                                                    ? 'bg-accent'
                                                    : 'hover:bg-accent/60'
                                            }`}
                                        >

                                            <button
                                                onClick={() =>
                                                    handleSelectConversation(id)
                                                }
                                                className="flex-1 min-w-0 text-left px-3 py-3"
                                            >

                                                <div className="flex items-center gap-2">

                                                    <ChatBubbleLeftIcon
                                                        className="h-4 w-4 shrink-0 text-muted-foreground"
                                                    />

                                                    <span className="truncate text-sm text-foreground">
                                                        {title}
                                                    </span>

                                                </div>

                                            </button>

                                            <button
                                                onClick={() =>
                                                    handleDeleteConversation(id)
                                                }
                                                disabled={loading}
                                                className="mr-2 p-1.5 rounded-md opacity-0 group-hover:opacity-100 hover:bg-destructive/10 hover:text-destructive disabled:opacity-30"
                                                title="Delete conversation"
                                            >
                                                <TrashIcon className="h-4 w-4" />
                                            </button>

                                        </div>
                                    );
                                })}

                            </div>

                        </div>

                    </aside>
                )}

                {/* =====================================================
                    MAIN CHAT
                ====================================================== */}

                <main className="flex-1 flex flex-col min-w-0">

                    {/* Chat header */}
                    <div className="h-16 shrink-0 border-b border-border flex items-center justify-between px-5">

                        <div className="flex items-center gap-3">

                            <button
                                onClick={() =>
                                    setSidebarOpen((prev) => !prev)
                                }
                                className="p-2 rounded-lg hover:bg-accent"
                                title={
                                    sidebarOpen
                                        ? 'Hide sidebar'
                                        : 'Show sidebar'
                                }
                            >
                                {sidebarOpen ? (
                                    <PanelLeftClose className="h-5 w-5" />
                                ) : (
                                    <PanelLeftOpen className="h-5 w-5" />
                                )}
                            </button>

                            <div>
                                <h2 className="font-semibold text-foreground">
                                    {conversationId
                                        ? 'Chat'
                                        : 'New Chat'}
                                </h2>

                                {conversationId && (
                                    <p className="text-xs text-muted-foreground">
                                        Conversation
                                    </p>
                                )}
                            </div>

                        </div>

                        {/* Provider / Model */}
                        <div className="flex items-center gap-2">

                            <select
                                value={aiProvider}
                                disabled={loading}
                                onChange={(e) => {

                                    const provider =
                                        e.target.value;

                                    setAiProvider(provider);

                                    setModelName(
                                        availableModels[
                                            provider
                                            ]?.[0] || ''
                                    );
                                }}
                                className="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground"
                            >

                                {Object.keys(
                                    availableModels
                                ).map((provider) => (
                                    <option
                                        key={provider}
                                        value={provider}
                                    >
                                        {provider}
                                    </option>
                                ))}

                            </select>

                            <select
                                value={modelName}
                                disabled={loading}
                                onChange={(e) =>
                                    setModelName(
                                        e.target.value
                                    )
                                }
                                className="rounded-lg border border-input bg-background px-3 py-2 text-sm text-foreground max-w-52"
                            >

                                {(
                                    availableModels[
                                        aiProvider
                                        ] || []
                                ).map((model: string) => (
                                    <option
                                        key={model}
                                        value={model}
                                    >
                                        {model}
                                    </option>
                                ))}

                            </select>

                        </div>

                    </div>

                    {/* =================================================
                        MESSAGES
                    ================================================== */}

                    <div className="flex-1 overflow-y-auto">

                        {messages.length === 0 ? (

                            <div className="h-full flex items-center justify-center">

                                <div className="text-center max-w-md px-6">

                                    <div className="mx-auto mb-5 h-16 w-16 rounded-full bg-primary/10 flex items-center justify-center">

                                        <ChatBubbleLeftIcon
                                            className="h-8 w-8 text-primary"
                                        />

                                    </div>

                                    <h1 className="text-2xl font-bold text-foreground mb-2">
                                        Ask your documents anything
                                    </h1>

                                    <p className="text-muted-foreground">
                                        Ask questions about the documents
                                        you've uploaded and get answers
                                        powered by AI.
                                    </p>

                                </div>

                            </div>

                        ) : (

                            <div className="max-w-4xl mx-auto w-full px-6 py-8 space-y-6">

                                {messages.map(
                                    (message, index) => {

                                        const isUser =
                                            message.messageType ===
                                            'USER';

                                        return (
                                            <div
                                                key={index}
                                                className={`flex ${
                                                    isUser
                                                        ? 'justify-end'
                                                        : 'justify-start'
                                                }`}
                                            >

                                                <div
                                                    className={`max-w-[80%] ${
                                                        isUser
                                                            ? 'items-end'
                                                            : 'items-start'
                                                    } flex flex-col`}
                                                >

                                                    <div
                                                        className={`text-xs font-medium mb-1 ${
                                                            isUser
                                                                ? 'text-muted-foreground'
                                                                : 'text-primary'
                                                        }`}
                                                    >
                                                        {isUser
                                                            ? 'You'
                                                            : '🤖'}
                                                    </div>

                                                    <div
                                                        className={`px-4 py-3 rounded-2xl whitespace-pre-wrap leading-relaxed ${
                                                            isUser
                                                                ? 'bg-primary text-primary-foreground rounded-br-md'
                                                                : 'bg-muted text-foreground rounded-bl-md'
                                                        }`}
                                                    >
                                                        {message.text}

                                                        {/* Streaming cursor */}
                                                        {!isUser &&
                                                            loading &&
                                                            index ===
                                                            messages.length -
                                                            1 && (
                                                                <span className="inline-block w-2 h-4 ml-1 align-middle bg-current animate-pulse" />
                                                            )}
                                                    </div>

                                                </div>

                                            </div>
                                        );
                                    }
                                )}

                                <div ref={messagesEndRef} />

                            </div>

                        )}

                    </div>

                    {/* Error */}
                    {error && (
                        <div className="px-6">

                            <div className="max-w-4xl mx-auto bg-destructive/10 border border-destructive/20 rounded-lg p-3 mb-3">

                                <p className="text-sm text-destructive">
                                    {error}
                                </p>

                            </div>

                        </div>
                    )}

                    {/* =================================================
                        INPUT
                    ================================================== */}

                    <div className="border-t border-border bg-background p-4">

                        <form
                            onSubmit={handleSearch}
                            className="max-w-4xl mx-auto"
                        >

                            <div className="relative flex items-end gap-3">

                                <div className="relative flex-1">

                                    <textarea
                                        value={question}
                                        onChange={(e) =>
                                            setQuestion(
                                                e.target.value
                                            )
                                        }
                                        onKeyDown={(e) => {

                                            /*
                                             * Enter = send
                                             * Shift + Enter = newline
                                             */
                                            if (
                                                e.key === 'Enter' &&
                                                !e.shiftKey
                                            ) {
                                                e.preventDefault();

                                                handleSearch(e);
                                            }
                                        }}
                                        rows={1}
                                        disabled={loading}
                                        placeholder="Ask anything about your documents..."
                                        className="w-full resize-none rounded-xl border border-input bg-background px-4 py-3 pr-12 text-foreground focus:outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                                    />

                                </div>

                                {loading ? (

                                    <button
                                        type="button"
                                        onClick={handleCancel}
                                        className="shrink-0 h-11 w-11 flex items-center justify-center rounded-xl bg-destructive text-destructive-foreground hover:bg-destructive/90"
                                        title="Stop generating"
                                    >
                                        <Square
                                            className="h-4 w-4 fill-current"
                                        />
                                    </button>

                                ) : (

                                    <button
                                        type="submit"
                                        disabled={
                                            !question.trim() ||
                                            !aiProvider ||
                                            !modelName
                                        }
                                        className="shrink-0 h-11 w-11 flex items-center justify-center rounded-xl bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
                                        title="Send"
                                    >
                                        <SendHorizonalIcon className="h-5 w-5" />
                                    </button>

                                )}

                            </div>

                            <div className="text-center mt-2 text-xs text-muted-foreground">
                                Enter to send · Shift + Enter for new line
                            </div>

                        </form>

                    </div>

                </main>

            </div>

        </LayoutContainer>
    );
};

export default Chat;