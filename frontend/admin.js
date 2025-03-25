document.addEventListener("DOMContentLoaded", () => {
  const treeForm = document.getElementById("tree-form");
  const pendingTreesDiv = document.getElementById("pending-trees");

  // Fetch trees and display
  async function fetchTrees() {
    try {
      const response = await fetch("http://localhost:8080/api/trees/list");
      const trees = await response.json();

      pendingTreesDiv.innerHTML = "";

      if (trees.length === 0) {
        pendingTreesDiv.innerHTML = "<p>No trees submitted yet.</p>";
        return;
      }

      const list = document.createElement("ul");

      trees.forEach((tree) => {
        const item = document.createElement("li");
        item.textContent = `${tree.name} — (${tree.latitude}, ${tree.longitude})`;
        list.appendChild(item);
      });

      pendingTreesDiv.appendChild(list);
    } catch (error) {
      console.error("Error fetching trees:", error);
      pendingTreesDiv.innerHTML = "<p>Error loading trees.</p>";
    }
  }

  // Submit new tree
  treeForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const formData = new FormData(treeForm);
    const fileInput = document.getElementById("treeImage");

    // Optionally handle image upload later
    const hasImage = fileInput.files.length > 0;

    try {
      const response = await fetch("http://localhost:8080/api/trees/add", {
        method: "POST",
        body: formData
      });

      if (response.ok) {
        alert("Tree submitted successfully!");
        treeForm.reset();
        fetchTrees();
      } else {
        alert("Error: Failed to submit the tree.");
        console.error("Response status:", response.status);
      }
    } catch (err) {
      console.error("Request error:", err);
      alert("An error occurred while adding the tree.");
    }
  });

  fetchTrees();
});
