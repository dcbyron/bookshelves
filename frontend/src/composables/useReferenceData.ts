import { useQuery } from "@tanstack/vue-query";

import { fetchCollections, fetchShelves } from "../api/catalog";

export function useReferenceData() {
  const collectionsQuery = useQuery({
    queryKey: ["collections"],
    queryFn: fetchCollections
  });

  const shelvesQuery = useQuery({
    queryKey: ["shelves"],
    queryFn: fetchShelves
  });

  return {
    collectionsQuery,
    shelvesQuery
  };
}
