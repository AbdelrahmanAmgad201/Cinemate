import { forwardRef, useId } from 'react';
import PropTypes from 'prop-types';
import './style/Input.css';

const Input = forwardRef(function Input(
    { label, error, helperText, icon, rightIcon, onRightIconClick, rightIconLabel, className = '', id, ...rest },
    ref
) {
    const generatedId = useId();
    const inputId = id || generatedId;
    const describedById = error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined;

    return (
        <div className={`field ${className}`}>
            {label && <label className="field__label" htmlFor={inputId}>{label}</label>}
            <div className={`field__control ${icon ? 'field__control--with-icon' : ''} ${rightIcon ? 'field__control--with-right-icon' : ''} ${error ? 'field__control--error' : ''}`}>
                {icon && <span className="field__icon">{icon}</span>}
                <input
                    ref={ref}
                    id={inputId}
                    className="field__input"
                    aria-invalid={!!error}
                    aria-describedby={describedById}
                    {...rest}
                />
                {rightIcon && (
                    onRightIconClick ? (
                        <button type="button" className="field__right-icon field__right-icon--button" onClick={onRightIconClick} aria-label={rightIconLabel}>
                            {rightIcon}
                        </button>
                    ) : (
                        <span className="field__right-icon">{rightIcon}</span>
                    )
                )}
            </div>
            {error && <p className="field__message field__message--error" id={`${inputId}-error`}>{error}</p>}
            {!error && helperText && <p className="field__message" id={`${inputId}-helper`}>{helperText}</p>}
        </div>
    );
});

Input.propTypes = {
    label: PropTypes.string,
    error: PropTypes.string,
    helperText: PropTypes.string,
    icon: PropTypes.node,
    rightIcon: PropTypes.node,
    onRightIconClick: PropTypes.func,
    rightIconLabel: PropTypes.string,
    className: PropTypes.string,
    id: PropTypes.string,
};

export default Input;
