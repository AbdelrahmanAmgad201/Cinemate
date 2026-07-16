import PropTypes from 'prop-types';
import './style/Card.css';

export default function Card({ interactive = false, padding = 'md', className = '', children, ...rest }) {
    const classes = [
        'card',
        `card--padding-${padding}`,
        interactive ? 'card--interactive' : '',
        className,
    ].filter(Boolean).join(' ');

    return <div className={classes} {...rest}>{children}</div>;
}

Card.propTypes = {
    interactive: PropTypes.bool,
    padding: PropTypes.oneOf(['none', 'sm', 'md', 'lg']),
    className: PropTypes.string,
    children: PropTypes.node,
};
