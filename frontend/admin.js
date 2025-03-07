document.addEventListener("DOMContentLoaded", () => {
    const pendingTreesContainer = document.getElementById("pending-trees");
    const treeForm = document.getElementById("tree-form");

    // Mock data - In a real app, this data would come from the backend
    const pendingTrees = [
        { id: 1, name: "Apple Tree", location: "47.629797, -122.366229", image: "", status: "Pending" },
        { id: 2, name: "Cherry Tree", location: "47.610136, -122.342057", image: "", status: "Pending" }
    ];

    // Render pending trees
    renderPendingTrees(pendingTrees);

    // Function to render pending trees
    function renderPendingTrees(trees) {
        pendingTreesContainer.innerHTML = ""; // Clear existing items
        trees.forEach(tree => {
            const treeElement = document.createElement("div");
            treeElement.className = "tree-item";
            treeElement.innerHTML = `
                <p><strong>${tree.name}</strong> - Location: ${tree.location}</p>
                ${tree.image ? `<img src="${tree.image}" alt="${tree.name}" class="tree-image">` : ""}
                <button onclick="approveTree(${tree.id})">Approve</button>
                <button onclick="rejectTree(${tree.id})">Reject</button>
            `;
            pendingTreesContainer.appendChild(treeElement);
        });
    }

    // Form submission to add new tree
    treeForm.addEventListener("submit", (event) => {
        event.preventDefault();
        
        const treeName = document.getElementById("treeName").value;
        const latitude = document.getElementById("latitude").value;
        const longitude = document.getElementById("longitude").value;
        const treeImage = document.getElementById("treeImage").files[0];

        if (treeName && latitude && longitude && treeImage) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const newTree = {
                    id: pendingTrees.length + 1,
                    name: treeName,
                    location: `${latitude}, ${longitude}`,
                    image: e.target.result,
                    status: "Pending"
                };

                pendingTrees.push(newTree);
                renderPendingTrees(pendingTrees);

                alert(`New tree "${treeName}" submitted for approval!`);

                // Reset form fields
                treeForm.reset();
            };

            reader.readAsDataURL(treeImage);
        } else {
            alert("Please fill in all fields and add an image.");
        }
    });
});

// Approve tree function
function approveTree(id) {
    alert(`Tree with ID ${id} approved!`);
    // TODO: Send approval to backend
}

// Reject tree function
function rejectTree(id) {
    alert(`Tree with ID ${id} rejected!`);
    // TODO: Send rejection to backend
}
