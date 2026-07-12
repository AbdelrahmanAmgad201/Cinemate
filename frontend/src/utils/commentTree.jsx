// Owner ids are plain numeric user ids now (was a 24-char ObjectId hex fabricated
// from the user id, which had to be parsed back with base-16). Just coerce to Number.
export const normalizeId = (id) => {
    if (id === null || id === undefined) return null;
    const numeric = Number(id);
    return Number.isNaN(numeric) ? null : numeric;
};

export const computeTotalComments = (arr) => {
    const countRepliesRecursive = (items) => (items || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
    return (arr || []).reduce((sum, it) => sum + 1 + countRepliesRecursive(it.replies || []), 0);
};
