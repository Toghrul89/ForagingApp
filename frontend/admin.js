document.addEventListener("DOMContentLoaded", () => {
    const pendingTreesContainer = document.getElementById("pending-trees");
    const treeForm = document.getElementById("tree-form");

    // Function to fetch pending trees from the backend
    async function fetchPendingTrees() {
        try {
            let response = await fetch("http://localhost:8080/api/trees/list");
            let trees = await response.json();
            renderPendingTrees(trees);
        } catch (error) {
            console.error("Error fetching trees:", error);
        }
    }

    // Function to render pending trees in the admin panel
    function renderPendingTrees(trees) {
        pendingTreesContainer.innerHTML = "";
        trees.forEach(tree => {
            const treeElement = document.createElement("div");
            treeElement.className = "tree-item";
            treeElement.innerHTML = `
                <p><strong>${tree.name}</strong> - Location: ${tree.location}</p>
                <img src="http://localhost:8080${tree.imageUrl}" alt="${tree.name}" class="tree-image">
                <button onclick="approveTree('${tree.id}')">Approve</button>
                <button onclick="rejectTree('${tree.id}')">Reject</button>
            `;
            pendingTreesContainer.appendChild(treeElement);
        });
    }

    // Form submission to add a new tree
    treeForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        
        const treeName = document.getElementById("treeName").value;
        const latitude = document.getElementById("latitude").value;
        const longitude = document.getElementById("longitude").value;
        const treeImage = document.getElementById("treeImage").files[0];

        if (treeName && latitude && longitude && treeImage) {
            let formData = new FormData();
            formData.append("name", treeName);
            formData.append("location", `${latitude}, ${longitude}`);
            formData.append("image", treeImage);

            try {
                let response = await fetch("http://localhost:8080/api/trees/add", {
                    method: "POST",
                    body: formData
                });

                if (response.ok) {
                    alert("Tree added successfully!");
                    treeForm.reset();
                    fetchPendingTrees(); // Refresh pending trees
                } else {
                    alert("Failed to add tree.");
                }
            } catch (error) {
                console.error("Error:", error);
                alert("An error occurred while adding the tree.");
            }
        } else {
            alert("Please fill in all fields and add an image.");
        }
    });

    // Approve tree function
    async function approveTree(id) {
        try {
            let response = await fetch(`http://localhost:8080/api/trees/approve/${id}`, {
                method: "PUT"
            });

            if (response.ok) {
                alert(`Tree with ID ${id} approved!`);
                fetchPendingTrees(); // Refresh list
            } else {
                alert("Failed to approve tree.");
            }
        } catch (error) {
            console.error("Error:", error);
        }
    }

    // Reject tree function
    async function rejectTree(id) {
        try {
            let response = await fetch(`http://localhost:8080/api/trees/reject/${id}`, {
                method: "DELETE"
            });

            if (response.ok) {
                alert(`Tree with ID ${id} rejected!`);
                fetchPendingTrees(); // Refresh list
            } else {
                alert("Failed to reject tree.");
            }
        } catch (error) {
            console.error("Error:", error);
        }
    }

    // Fetch trees when the page loads
    fetchPendingTrees();
});
