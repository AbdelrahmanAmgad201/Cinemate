import { forwardRef, useId } from 'react';
import PropTypes from 'prop-types';
import { ChevronDown } from 'lucide-react';
import './style/Select.css';

const Select = forwardRef(function Select(
    { label, error, options, placeholder, className = '', id, ...rest },
    ref
) {
    const generatedId = useId();
    const selectId = id || generatedId;

    return (
        <div className={`field ${className}`}>
            {label && <label className="field__label" htmlFor={selectId}>{label}</label>}
            <div className={`select ${error ? 'field__control--error' : ''}`}>
                <select ref={ref} id={selectId} className="select__control" aria-invalid={!!error} {...rest}>
                    {placeholder && <option value="" disabled>{placeholder}</option>}
                    {options.map(opt => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                    ))}
                </select>
                <ChevronDown className="select__chevron" size={16} aria-hidden="true" />
            </div>
            {error && <p className="field__message field__message--error">{error}</p>}
        </div>
    );
});

Select.propTypes = {
    label: PropTypes.string,
    error: PropTypes.string,
    placeholder: PropTypes.string,
    options: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
        label: PropTypes.string.isRequired,
    })).isRequired,
    className: PropTypes.string,
    id: PropTypes.string,
};

export default Select;
