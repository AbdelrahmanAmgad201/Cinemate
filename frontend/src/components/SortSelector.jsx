import React, {useState} from "react";
import "./style/SortSelector.css"
import {IoIosArrowDown, IoIosPerson} from "react-icons/io";

export default function SortSelector({ currentSort, options, onSortChange }){
    const [isOpen, setIsOpen] = useState(false);

    const handleChangeSort = (option) => {
        onSortChange(option);
        setIsOpen(false);
    };

    return (
        <div className="feed-sort-bar">
            <span className="sort-label">Sort By:</span>

            <div className="sort-dropdown-container">
                <button
                    className="sort-trigger"
                    onClick={() => setIsOpen(!isOpen)}
                    onBlur={() => setTimeout(() => setIsOpen(false), 200)} // Close when clicking away
                >
                    <span className="sort-icon">{currentSort.icon}</span>
                    <span className="sort-text">{currentSort.label}</span>
                    <IoIosArrowDown className={`sort-arrow ${isOpen ? 'open' : ''}`} />
                </button>

                {isOpen && (
                    <div className="sort-dropdown-menu">
                        {options.map((option) => (
                            <div
                                key={option.value}
                                className={`sort-option ${currentSort.value === option.value ? 'selected' : ''}`}
                                onClick={() => {
                                    handleChangeSort(option);
                                    setIsOpen(false);
                                }}
                            >
                                <span className="sort-option-icon">{option.icon}</span>
                                {option.label}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}