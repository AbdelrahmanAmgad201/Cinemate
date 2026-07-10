export const normalizeId = (id) => {
    if (id === null || id === undefined) return null;
    if (typeof id === 'number') return id;
    const numeric = Number(id);
    if (!Number.isNaN(numeric)) return numeric;
    const hex = parseInt(id, 16);
    return Number.isNaN(hex) ? null : hex;
};

export const computeTotalComments = (arr) => {
    const countRepliesRecursive = (items) => (items || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
    return (arr || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
};
