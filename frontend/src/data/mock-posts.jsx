// Generate a large number of mock posts for testing infinite scroll
function generateMockPosts(count = 100) {
    const posts = [];
    for (let i = 1; i <= count; i++) {
        const up = Math.floor(Math.random() * 300);
        const down = Math.floor(Math.random() * 50);
        const voteCount = Math.max(0, up - down);

        posts.push({
            postId: String(i),
            title: `Mock post #${i}`,
            text: `This is example content for mock post #${i}. It contains some text to simulate body content.`,
            upvotes: up,
            downvotes: down,
            votes: voteCount,
            commentCount: Math.floor(Math.random() * 40),
            firstName: `User${i}`,
            lastName: `Mock`,
            avatar: null,
            time: new Date(Date.now() - Math.floor(Math.random() * 1000 * 60 * 60 * 24 * 30)).toLocaleString()
        });
    }
    return posts;
}

const mockPosts = generateMockPosts(200);

export function getMockPosts({ page = 0, size = 10 } = {}) {
    const total = mockPosts.length;
    const totalPages = Math.ceil(total / size);
    const start = page * size;
    const end = start + size;
    const content = mockPosts.slice(start, end);
    return { content, page, totalPages, total };
}

export default mockPosts;
