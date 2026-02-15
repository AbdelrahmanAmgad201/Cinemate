export function formatRuntime(minutes) {
    if (!minutes) return "";
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return `${h}h ${m}m`;
}

export function timeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;

    const seconds = Math.floor(diffMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    const months = Math.floor(days / 30);
    const years = Math.floor(days / 365);

    if (seconds < 60) return "Just now";
    if (minutes < 60) return `${minutes} min ago`;
    if (hours < 24)   return `${hours} hour${hours > 1 ? "s" : ""} ago`;
    if (days < 30)    return `${days} day${days > 1 ? "s" : ""} ago`;
    if (months < 12)  return `${months} month${months > 1 ? "s" : ""} ago`;

    return `${years} year${years > 1 ? "s" : ""} ago`;
}

export const formatCount = (num) => {
    if (num === null || num === undefined) return '0';
    const n = Number(num);
    if (Number.isNaN(n)) return String(num);
    const abs = Math.abs(n);

    const truncToOne = (value, divisor) => {
        const val = value / divisor;
        const truncated = value >= 0 ? Math.floor(val * 10) / 10 : Math.ceil(val * 10) / 10;
        let s = String(truncated);
        if (s.indexOf('.') >= 0) s = s.replace(/\.0$/, '');
        return s;
    };

    if (abs >= 1_000_000_000) return truncToOne(n, 1_000_000_000) + 'B';
    if (abs >= 1_000_000) return truncToOne(n, 1_000_000) + 'M';
    if (abs >= 1_000) return truncToOne(n, 1_000) + 'k';
    return String(n);
};
