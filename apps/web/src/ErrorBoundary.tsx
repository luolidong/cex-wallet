import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Alert, Button } from 'antd';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  error?: Error;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = {};

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Unhandled UI error', error, errorInfo);
  }

  render() {
    if (this.state.error) {
      return (
        <div className="page-error">
          <Alert
            type="error"
            showIcon
            message="页面发生错误"
            description={this.state.error.message}
            action={
              <Button size="small" onClick={() => window.location.reload()}>
                刷新
              </Button>
            }
          />
        </div>
      );
    }

    return this.props.children;
  }
}
