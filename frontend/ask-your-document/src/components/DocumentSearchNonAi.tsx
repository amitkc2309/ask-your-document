import {useState} from 'react';
import {ChatBubbleLeftIcon, DocumentTextIcon, PaperAirplaneIcon,} from '@heroicons/react/24/outline';
import config from '../config/config';
import api from '../api';
import LayoutContainer from "./ui/LayoutContainer.tsx";

const DocumentSearch = ({ question, setQuestion, searchResults, setSearchResults }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSearch = async (e) => {
        e.preventDefault();
        if (!question.trim()) return;

        setLoading(true);
        setError('');

        try {
            const response = await api.post(`${config.apiUrl}/manage/search/docs`, {
                keyword: question,
                maxResults: 5,
                snippetLength: 200
            });

            setSearchResults(response.data);
        } catch (err) {
            setError(err.response?.data?.message || 'Error performing search');
        } finally {
            setLoading(false);
        }
    };

    return (
        <LayoutContainer>
            <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-6 mb-8">
                <div className="flex items-center mb-4">
                    <h2 className="text-2xl font-bold text-gray-900">Search Documents</h2>
                </div>
                <p className="text-gray-600 mb-6">
                    Search across all of your documents. Our systems will search through them and find relevant information.
                </p>
                <form onSubmit={handleSearch} className="relative">
                    <div className="flex items-center gap-4">
                        <div className="flex-1 relative">
                            <input
                                type="text"
                                value={question}
                                onChange={(e) => setQuestion(e.target.value)}
                                placeholder="What would you like to know?"
                                className="w-full pl-12 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-sm"
                            />
                            <ChatBubbleLeftIcon className="h-6 w-6 text-gray-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
                        </div>
                        <button
                            type="submit"
                            disabled={loading}
                            className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            <span className="mr-2">Search</span>
                            <PaperAirplaneIcon className="h-5 w-5 transform rotate-90" />
                        </button>
                    </div>
                </form>
            </div>

            {loading && (
                <div className="flex flex-col items-center justify-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
                    <p className="mt-4 text-gray-600">Searching through documents...</p>
                </div>
            )}

            {error && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
                    <p className="text-red-600">{error}</p>
                </div>
            )}

            {searchResults && !loading && (
                <div className="space-y-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">
                        Search Results
                    </h3>

                    {searchResults.snippets?.map((result, index) => (
                        <div
                            key={index}
                            className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-md transition-shadow"
                        >
                            <div className="flex items-start">
                                <DocumentTextIcon className="h-6 w-6 text-blue-500 mt-1 flex-shrink-0" />
                                <div className="ml-4 flex-1">
                                    <h4 className="text-lg font-semibold text-gray-900">
                                        {result.documentTitle}
                                    </h4>
                                    <p className="text-sm text-gray-600 mb-3">
                                        By {result.author}
                                    </p>
                                    <div className="bg-gray-50 rounded-lg p-4">
                                        <p className="text-gray-700 whitespace-pre-line">
                                            <div dangerouslySetInnerHTML={{ __html: result.snippet }} />
                                        </p>
                                    </div>
                                    <div className="mt-3 flex justify-between items-center">
                    <span className="text-sm text-gray-500">
                      Relevance Score: {Math.round(result.relevanceScore * 100)}%
                    </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}

                    {searchResults.snippets?.length === 0 && (
                        <div className="text-center py-12 bg-gray-50 rounded-lg">
                            <DocumentTextIcon className="mx-auto h-12 w-12 text-gray-400" />
                            <h3 className="mt-2 text-lg font-medium text-gray-900">
                                No matching results
                            </h3>
                            <p className="mt-1 text-gray-500">
                                Try rephrasing your question or using different keywords
                            </p>
                        </div>
                    )}
                </div>
            )}
        </LayoutContainer>
    );
};

export default DocumentSearch;