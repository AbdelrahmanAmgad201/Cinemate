export function generateColorFromUserId(userId) {
    const colors = [
        "#FF6B6B", // Red
        "#4ECDC4", // Teal
        "#FFE66D", // Yellow
        "#95E1D3", // Mint
        "#F38181", // Pink
        "#AA96DA", // Purple
        "#FCBAD3", // Light Pink
        "#A8D8EA", // Sky Blue
        "#FFD93D", // Gold
        "#6BCB77", // Green
        "#4D96FF", // Blue
        "#FF8C42", // Orange
        "#C77DFF", // Lavender
        "#52B788", // Emerald
        "#06FFA5", // Neon Green
        "#E0AFA0"  // Peach
    ];
    
    if (!userId) return colors[0];
    
    const hash = userId.toString().split('').reduce((acc, char) => {
        return char.charCodeAt(0) + ((acc << 5) - acc);
    }, 0);
    
    return colors[Math.abs(hash) % colors.length];
}