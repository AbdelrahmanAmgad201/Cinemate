import { forwardRef } from 'react';
import PropTypes from 'prop-types';
import { Loader2 } from 'lucide-react';
import './style/Button.css';

/**
 * Core button primitive. Every clickable action in the app should render
 * through this (or IconButton) so hover/focus/disabled/loading states stay
 * consistent instead of being reinvented per page.
 */
const Button = forwardRef(function Button(
    {
        variant = 'primary',
        size = 'md',
        fullWidth = false,
        loading = false,
        disabled = false,
        icon = null,
        iconPosition = 'left',
        type = 'button',
        className = '',
        children,
        ...rest
    },
    ref
) {
    const classes = [
        'btn',
        `btn--${variant}`,
        `btn--${size}`,
        fullWidth ? 'btn--full' : '',
        loading ? 'btn--loading' : '',
        className,
    ].filter(Boolean).join(' ');

    return (
        <button
            ref={ref}
            type={type}
            className={classes}
            disabled={disabled || loading}
            aria-busy={loading || undefined}
            {...rest}
        >
            {loading && <Loader2 className="btn__spinner" size={16} aria-hidden="true" />}
            {!loading && icon && iconPosition === 'left' && <span className="btn__icon">{icon}</span>}
            {children && <span className="btn__label">{children}</span>}
            {!loading && icon && iconPosition === 'right' && <span className="btn__icon">{icon}</span>}
        </button>
    );
});

Button.propTypes = {
    variant: PropTypes.oneOf(['primary', 'secondary', 'ghost', 'danger']),
    size: PropTypes.oneOf(['sm', 'md', 'lg']),
    fullWidth: PropTypes.bool,
    loading: PropTypes.bool,
    disabled: PropTypes.bool,
    icon: PropTypes.node,
    iconPosition: PropTypes.oneOf(['left', 'right']),
    type: PropTypes.oneOf(['button', 'submit', 'reset']),
    className: PropTypes.string,
    children: PropTypes.node,
};

export default Button;
