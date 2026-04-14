variable "region" {
  default = "ap-southeast-2"
}

variable "cluster_name" {
  default = "maplehub-platform"
}

variable "tags" {
  default = {
    Project     = "k8s-platform"
    Environment = "production"
    ManagedBy   = "terraform"
  }
}
