import { forwardRef } from 'react';
import PropTypes from 'prop-types';
import './style/IconButton.css';

/**
 * Icon-only button (no visible label). `label` is required and rendered as
 * aria-label so icon-only controls stay accessible to screen readers.
 */
const IconButton = forwardRef(function IconButton(
    { variant = 'ghost', size = 'md', active = false, label, className = '', children, ...rest },
    ref
) {
    const classes = [
        'icon-btn',
        `icon-btn--${variant}`,
        `icon-btn--${size}`,
        active ? 'icon-btn--active' : '',
        className,
    ].filter(Boolean).join(' ');

    return (
        <button ref={ref} type="button" className={classes} aria-label={label} title={label} {...rest}>
            {children}
        </button>
    );
});

IconButton.propTypes = {
    variant: PropTypes.oneOf(['ghost', 'solid']),
    size: PropTypes.oneOf(['sm', 'md', 'lg']),
    active: PropTypes.bool,
    label: PropTypes.string.isRequired,
    className: PropTypes.string,
    children: PropTypes.node,
};

export default IconButton;
