import React from 'react';
import { AlertTriangle } from 'lucide-react';
import EmptyState from './ui/EmptyState.jsx';

// Without this, an uncaught render error anywhere in the tree crashes the
// whole SPA to a blank white screen with no recovery path. Must be a class
// component — React has no hook equivalent for getDerivedStateFromError/componentDidCatch.
export default class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError() {
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        console.error('Unhandled render error:', error, errorInfo);
    }

    render() {
        if (this.state.hasError) {
            return (
                <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <EmptyState
                        icon={<AlertTriangle size={28} />}
                        title="Something went wrong"
                        description="An unexpected error occurred. Reloading the page usually fixes it."
                        actionLabel="Reload"
                        onAction={() => window.location.reload()}
                    />
                </div>
            );
        }
        return this.props.children;
    }
}
