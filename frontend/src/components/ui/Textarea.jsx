import { forwardRef, useId } from 'react';
import PropTypes from 'prop-types';
import './style/Input.css';

const Textarea = forwardRef(function Textarea(
    { label, error, helperText, maxLength, value, className = '', id, ...rest },
    ref
) {
    const generatedId = useId();
    const inputId = id || generatedId;
    const describedById = error ? `${inputId}-error` : helperText ? `${inputId}-helper` : undefined;

    return (
        <div className={`field ${className}`}>
            {label && <label className="field__label" htmlFor={inputId}>{label}</label>}
            <div className={`field__control ${error ? 'field__control--error' : ''}`}>
                <textarea
                    ref={ref}
                    id={inputId}
                    className="field__textarea"
                    maxLength={maxLength}
                    value={value}
                    aria-invalid={!!error}
                    aria-describedby={describedById}
                    {...rest}
                />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div>
                    {error && <p className="field__message field__message--error" id={`${inputId}-error`}>{error}</p>}
                    {!error && helperText && <p className="field__message" id={`${inputId}-helper`}>{helperText}</p>}
                </div>
                {maxLength && <span className="field__counter">{(value?.length ?? 0)}/{maxLength}</span>}
            </div>
        </div>
    );
});

Textarea.propTypes = {
    label: PropTypes.string,
    error: PropTypes.string,
    helperText: PropTypes.string,
    maxLength: PropTypes.number,
    value: PropTypes.string,
    className: PropTypes.string,
    id: PropTypes.string,
};

export default Textarea;
