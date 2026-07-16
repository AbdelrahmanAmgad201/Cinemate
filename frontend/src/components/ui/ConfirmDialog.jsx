import PropTypes from 'prop-types';
import Modal from './Modal.jsx';
import Button from './Button.jsx';

/**
 * Themed replacement for the ad hoc sweetalert2 confirmation popups used
 * across the app — same "are you sure?" job, but matches the design system
 * instead of sweetalert2's default look.
 */
export default function ConfirmDialog({
    open,
    onClose,
    onConfirm,
    title = 'Are you sure?',
    message,
    confirmLabel = 'Confirm',
    cancelLabel = 'Cancel',
    danger = false,
    loading = false,
}) {
    return (
        <Modal
            open={open}
            onClose={onClose}
            title={title}
            size="sm"
            footer={
                <>
                    <Button variant="ghost" onClick={onClose} disabled={loading}>{cancelLabel}</Button>
                    <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm} loading={loading}>
                        {confirmLabel}
                    </Button>
                </>
            }
        >
            <p style={{ margin: 0 }}>{message}</p>
        </Modal>
    );
}

ConfirmDialog.propTypes = {
    open: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
    title: PropTypes.string,
    message: PropTypes.string.isRequired,
    confirmLabel: PropTypes.string,
    cancelLabel: PropTypes.string,
    danger: PropTypes.bool,
    loading: PropTypes.bool,
};
